package com.mohamedzaitoon.linkifyall

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {

    // TODO: Update these with your actual GitHub details
    private const val GITHUB_USER = "mohamed-zaitoon"
    private const val GITHUB_REPO = "LinkifyAll" // Ensure this matches your repo name

    private const val API_URL = "https://api.github.com/repos/$GITHUB_USER/$GITHUB_REPO/releases/latest"

    fun checkForUpdate(context: Context, isManualCheck: Boolean = false) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val url = URL(API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val stream = connection.inputStream
                    val response = stream.bufferedReader().use { it.readText() }
                    val jsonResponse = JSONObject(response)

                    // Get latest version tag (e.g., "v2.5")
                    var latestVersion = jsonResponse.optString("tag_name", "")
                    // Remove 'v' prefix if it exists
                    latestVersion = latestVersion.replace("v", "", ignoreCase = true)

                    // Get current app version
                    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    val currentVersion = pInfo.versionName

                    // Simple string comparison.
                    // Note: For complex versioning (1.0.1 vs 1.0.10), use a dedicated comparator.
                    if (latestVersion.isNotEmpty() && latestVersion != currentVersion) {
                        val downloadUrl = jsonResponse.optString("html_url", "")
                        val releaseNotes = jsonResponse.optString("body", "No release notes provided.")

                        withContext(Dispatchers.Main) {
                            showUpdateDialog(context, latestVersion, releaseNotes, downloadUrl)
                        }
                    } else if (isManualCheck) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "You are using the latest version ($currentVersion)", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.e("UpdateChecker", "Failed to connect: ${connection.responseCode}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (isManualCheck) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to check for updates", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showUpdateDialog(context: Context, newVersion: String, changes: String, url: String) {
        if (context is Activity && !context.isFinishing) {
            AlertDialog.Builder(context)
                .setTitle("New Update Available ($newVersion)")
                .setMessage("Changelog:\n$changes")
                .setPositiveButton("Update") { _, _ ->
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(browserIntent)
                }
                .setNegativeButton("Later", null)
                .setCancelable(true)
                .show()
        }
    }
}