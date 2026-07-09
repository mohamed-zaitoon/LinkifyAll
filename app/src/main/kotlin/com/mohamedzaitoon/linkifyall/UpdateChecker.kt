package com.mohamedzaitoon.linkifyall

import android.content.Context
import android.content.pm.PackageInfo
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {

    // --- GitHub Config ---
    private const val GITHUB_USER = "mohamed-zaitoon"
    private const val GITHUB_REPO = "LinkifyAll"
    // API لجلب آخر إصدار (الأحدث زمنياً)
    private const val GITHUB_API_URL = "https://api.github.com/repos/$GITHUB_USER/$GITHUB_REPO/releases?per_page=1"
    private const val DEFAULT_GITHUB_URL = "https://github.com/$GITHUB_USER/$GITHUB_REPO"
    private const val DEFAULT_WEBSITE_URL = "https://linkifyall.mohamedzaitoon.com"

    // --- Firebase Keys ---
    private const val KEY_LATEST_VERSION_CODE = "latest_version_code"
    private const val KEY_LATEST_VERSION_NAME = "latest_version_name"
    private const val KEY_UPDATE_URL = "update_url" // قيمته ستكون AUTO
    private const val KEY_RELEASE_NOTES = "release_notes"

    private const val KEY_GITHUB_URL = "github_url"
    private const val KEY_WEBSITE_URL = "website_url"

    data class ConfigResult(
        val githubUrl: String,
        val websiteUrl: String,
        val updateAvailable: Boolean = false,
        val latestVersionName: String = "",
        val downloadUrl: String = "",
        val releaseNotes: String = ""
    )

    interface UpdateListener {
        fun onConfigFetched(result: ConfigResult)
        fun onError(error: String)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun checkForUpdate(context: Context, listener: UpdateListener) {
        val remoteConfig: FirebaseRemoteConfig = Firebase.remoteConfig

        val configSettings = remoteConfigSettings { minimumFetchIntervalInSeconds = 3600 }
        remoteConfig.setConfigSettingsAsync(configSettings)

        // القيم الافتراضية
        val defaults = mapOf(
            KEY_LATEST_VERSION_CODE to 0,
            KEY_LATEST_VERSION_NAME to "",
            KEY_UPDATE_URL to "AUTO", // الافتراضي هو البحث التلقائي
            KEY_GITHUB_URL to DEFAULT_GITHUB_URL,
            KEY_WEBSITE_URL to DEFAULT_WEBSITE_URL
        )
        remoteConfig.setDefaultsAsync(defaults)

        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                processFirebaseConfig(context, remoteConfig, listener)
            } else {
                listener.onError("Firebase Fetch Failed")
            }
        }
    }

    private fun processFirebaseConfig(
        context: Context,
        remoteConfig: FirebaseRemoteConfig,
        listener: UpdateListener
    ) {
        try {
            // 1. جلب البيانات الأساسية من Firebase
            val githubUrl = remoteConfig.getString(KEY_GITHUB_URL).ifBlank { DEFAULT_GITHUB_URL }
            val websiteUrl = remoteConfig.getString(KEY_WEBSITE_URL).ifBlank { DEFAULT_WEBSITE_URL }

            val latestCode = remoteConfig.getLong(KEY_LATEST_VERSION_CODE)
            val latestName = remoteConfig.getString(KEY_LATEST_VERSION_NAME).ifBlank { "new version" }
            val firebaseUpdateUrl = remoteConfig.getString(KEY_UPDATE_URL).trim()
            val notes = remoteConfig.getString(KEY_RELEASE_NOTES)

            // 2. التحقق من الإصدار
            val currentCode = getAppVersionCode(context)

            if (latestCode > currentCode) {
                // يوجد تحديث!

                // 3. التحقق هل الرابط مباشر أم تلقائي (AUTO)؟
                if (firebaseUpdateUrl.equals("AUTO", ignoreCase = true)) {
                    // اذهب لجلب الرابط من GitHub
                    fetchDynamicUrlFromGitHub { fetchedUrl, error ->
                        if (fetchedUrl != null) {
                            listener.onConfigFetched(
                                ConfigResult(githubUrl, websiteUrl, true, latestName, fetchedUrl, notes)
                            )
                        } else {
                            // فشل جلب الرابط من GitHub، نستخدم رابط الصفحة كاحتياطي
                            listener.onConfigFetched(
                                ConfigResult(githubUrl, websiteUrl, true, latestName, githubUrl, "$notes\n(Download from GitHub Page)")
                            )
                        }
                    }
                } else if (firebaseUpdateUrl.isNotBlank()) {
                    // الرابط مباشر من Firebase
                    listener.onConfigFetched(
                        ConfigResult(githubUrl, websiteUrl, true, latestName, firebaseUpdateUrl, notes)
                    )
                } else {
                    listener.onConfigFetched(
                        ConfigResult(githubUrl, websiteUrl, true, latestName, githubUrl, "$notes\n(Download from GitHub Page)")
                    )
                }
            } else {
                // لا يوجد تحديث
                listener.onConfigFetched(
                    ConfigResult(githubUrl, websiteUrl, updateAvailable = false)
                )
            }

        } catch (e: Exception) {
            listener.onError("Processing Error: ${e.message}")
        }
    }

    // دالة لجلب رابط الـ APK من GitHub API
    private fun fetchDynamicUrlFromGitHub(callback: (String?, String?) -> Unit) {
        scope.launch {
            try {
                val url = URL(GITHUB_API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                // Headers لتجنب الحظر
                connection.setRequestProperty("User-Agent", "LinkifyAll-App")
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")

                try {
                    if (connection.responseCode == 200) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        val releases = JSONArray(response)

                        if (releases.length() > 0) {
                            val latestJson = releases.getJSONObject(0)
                            val assets = latestJson.getJSONArray("assets")

                            var apkUrl = ""
                            // البحث عن أول ملف ينتهي بـ .apk
                            for (i in 0 until assets.length()) {
                                val asset = assets.getJSONObject(i)
                                if (asset.getString("name").endsWith(".apk")) {
                                    apkUrl = asset.getString("browser_download_url")
                                    break
                                }
                            }

                            // العودة بالنتيجة للـ Main Thread
                            withContext(Dispatchers.Main) {
                                if (apkUrl.isNotEmpty()) callback(apkUrl, null)
                                else callback(null, "No APK found in release")
                            }
                        } else {
                            withContext(Dispatchers.Main) { callback(null, "No releases found") }
                        }
                    } else {
                        withContext(Dispatchers.Main) { callback(null, "GitHub Error: ${connection.responseCode}") }
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { callback(null, e.message) }
            }
        }
    }

    private fun getAppVersionCode(context: Context): Long {
        return try {
            val pInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.longVersionCode
        } catch (e: Exception) { -1L }
    }
}
