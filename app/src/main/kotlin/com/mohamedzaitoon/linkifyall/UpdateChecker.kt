package com.mohamedzaitoon.linkifyall

import android.content.Context
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray // 👈 Ensure this import is present
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max

object UpdateChecker {

    private const val GITHUB_USER = "mohamed-zaitoon"
    private const val GITHUB_REPO = "LinkifyAll"

    // 👇 Change here: Removed "latest" and used the list, limiting to 1 item (the newest)
    private const val API_URL = "https://api.github.com/repos/$GITHUB_USER/$GITHUB_REPO/releases?per_page=1"

    interface UpdateListener {
        fun onUpdateAvailable(version: String, url: String, changes: String)
        fun onNoUpdate()
        fun onError(error: String)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun checkForUpdate(context: Context, listener: UpdateListener) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val url = URL(API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }

                    // 👇 Since the URL points to a list, the response is now a JSONArray, not a JSONObject
                    val releases = JSONArray(response)

                    if (releases.length() > 0) {
                        // Get the first release (chronologically newest, whether pre-release or stable)
                        val json = releases.getJSONObject(0)

                        var latestVersion = json.optString("tag_name", "").replace("v", "", true)

                        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                        val currentVersion = pInfo.versionName ?: "1.0"

                        // Compare versions
                        if (latestVersion.isNotEmpty() && isNewer(currentVersion, latestVersion)) {

                            val assets = json.getJSONArray("assets")
                            var downloadUrl = ""
                            for (i in 0 until assets.length()) {
                                val asset = assets.getJSONObject(i)
                                if (asset.getString("name").endsWith(".apk")) {
                                    downloadUrl = asset.getString("browser_download_url")
                                    break
                                }
                            }
                            if (downloadUrl.isEmpty()) downloadUrl = json.getString("html_url")

                            // Add a warning if it is a pre-release
                            val isPrerelease = json.optBoolean("prerelease", false)
                            var body = json.optString("body", "New update available!")
                            if (isPrerelease) {
                                body = "⚠️ [Pre-release]\n$body"
                            }

                            withContext(Dispatchers.Main) {
                                listener.onUpdateAvailable(latestVersion, downloadUrl, body)
                            }
                        } else {
                            withContext(Dispatchers.Main) { listener.onNoUpdate() }
                        }
                    } else {
                        withContext(Dispatchers.Main) { listener.onNoUpdate() }
                    }
                } else {
                    withContext(Dispatchers.Main) { listener.onError("Connection Failed: ${connection.responseCode}") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { listener.onError(e.message ?: "Unknown Error") }
            }
        }
    }

    private fun isNewer(current: String, latest: String): Boolean {
        try {
            // Clean numbers from any suffix like "-beta" to ensure correct comparison
            // Keep only numbers and dots (e.g., extracting 2.5 from 2.5-beta01)
            val cleanCurrent = current.replace(Regex("[^0-9.]"), "")
            val cleanLatest = latest.replace(Regex("[^0-9.]"), "")

            val currentParts = cleanCurrent.split(".").map { it.toIntOrNull() ?: 0 }
            val latestParts = cleanLatest.split(".").map { it.toIntOrNull() ?: 0 }
            val length = max(currentParts.size, latestParts.size)

            for (i in 0 until length) {
                val c = currentParts.getOrElse(i) { 0 }
                val l = latestParts.getOrElse(i) { 0 }

                if (l > c) return true
                if (l < c) return false
            }
        } catch (e: Exception) {
            return false
        }
        return false
    }
}