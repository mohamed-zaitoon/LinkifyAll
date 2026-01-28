package com.mohamedzaitoon.linkifyall

import android.content.Context
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max

object UpdateChecker {

    // بياناتك
    private const val GITHUB_USER = "mohamed-zaitoon"
    private const val GITHUB_REPO = "LinkifyAll"
    private const val API_URL = "https://api.github.com/repos/$GITHUB_USER/$GITHUB_REPO/releases/latest"

    // الواجهة
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
                    val json = JSONObject(response)

                    var latestVersion = json.optString("tag_name", "").replace("v", "", true)

                    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    // ✅ الإصلاح هنا: أضفنا ?: "1.0" لضمان أن النص ليس null
                    val currentVersion = pInfo.versionName ?: "1.0"

                    // المقارنة
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

                        val body = json.optString("body", "تحديث جديد متوفر!")

                        withContext(Dispatchers.Main) {
                            listener.onUpdateAvailable(latestVersion, downloadUrl, body)
                        }
                    } else {
                        withContext(Dispatchers.Main) { listener.onNoUpdate() }
                    }
                } else {
                    withContext(Dispatchers.Main) { listener.onError("Connection Failed: ${connection.responseCode}") }
                }
            } catch (e: Exception) {
                // ✅ إصلاح إضافي: ضمان أن رسالة الخطأ نصية دائماً
                withContext(Dispatchers.Main) { listener.onError(e.message ?: "Unknown Error") }
            }
        }
    }

    private fun isNewer(current: String, latest: String): Boolean {
        try {
            val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
            val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
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