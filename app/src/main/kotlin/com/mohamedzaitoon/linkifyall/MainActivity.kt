package com.mohamedzaitoon.linkifyall

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import java.io.File

class MainActivity : Activity() {

    private val GITHUB_URL = "https://github.com/mohamed-zaitoon/LinkifyAll"
    private val WEBSITE_URL = "https://mohamedzaitoon.com"

    // متغير لحفظ معرف التحميل
    private var downloadId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- Root Layout (خلفية رمادية فاتحة) ---
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#F2F4F8")) // رمادي فاتح جداً
            setPadding(60, 60, 60, 60)
        }

        // --- Card Container (البطاقة البيضاء) ---
        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(50, 80, 50, 80)

            // رسم الخلفية البيضاء مع زوايا دائرية
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 40f
                setStroke(2, Color.parseColor("#E0E0E0")) // حدود خفيفة
            }
            elevation = 10f // ظل
        }

        // 1. العنوان
        val titleView = TextView(this).apply {
            text = "LinkifyAll"
            textSize = 32f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#1A1A1A"))
            gravity = Gravity.CENTER
        }
        cardLayout.addView(titleView)

        // 2. حالة الموديول (Badge)
        val isActive = isModuleActive()
        val statusBgColor = if (isActive) "#E8F5E9" else "#FFEBEE" // خلفية خفيفة
        val statusTxtColor = if (isActive) "#2E7D32" else "#C62828" // نص غامق

        val statusBadge = TextView(this).apply {
            text = if (isActive) "Active ●" else "Inactive ●"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor(statusTxtColor))
            gravity = Gravity.CENTER
            setPadding(40, 15, 40, 15)

            // خلفية الـ Badge
            background = GradientDrawable().apply {
                setColor(Color.parseColor(statusBgColor))
                cornerRadius = 50f
            }

            // هوامش
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 30, 0, 50) }
        }
        cardLayout.addView(statusBadge)

        // 3. رقم الإصدار
        val versionInfo = try { packageManager.getPackageInfo(packageName, 0).versionName } catch (e: Exception) { "?" }
        val versionView = TextView(this).apply {
            text = "Version $versionInfo"
            textSize = 16f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
        }
        cardLayout.addView(versionView)

        // فاصل بسيط
        cardLayout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(100, 2).apply { setMargins(0, 40, 0, 40) }
            setBackgroundColor(Color.LTGRAY)
        })

        // 4. زر التحديث
        val updateButton = Button(this).apply {
            text = "Checking for updates..."
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            isEnabled = false // معطل حتى انتهاء الفحص
            background = getRoundedButtonDrawable("#BDBDBD") // رمادي مبدئياً

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 140
            ).apply { setMargins(20, 10, 20, 30) }
        }
        cardLayout.addView(updateButton)

        // 5. الروابط (GitHub & Website)
        val linksLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 40, 0, 0)
        }

        fun createIconLink(emoji: String, label: String, url: String) {
            val tv = TextView(this).apply {
                text = "$emoji $label"
                textSize = 14f
                setTextColor(Color.parseColor("#1976D2"))
                setPadding(30, 20, 30, 20)
                gravity = Gravity.CENTER
                setOnClickListener { openUrl(url) }
            }
            linksLayout.addView(tv)
        }

        createIconLink("🐙", "GitHub", GITHUB_URL)
        createIconLink("🌐", "Website", WEBSITE_URL)
        cardLayout.addView(linksLayout)

        // 6. حقوق المطور
        val devInfo = TextView(this).apply {
            text = "© Mohamed Zaitoon"
            textSize = 12f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 40, 0, 0)
        }
        cardLayout.addView(devInfo)

        // إضافة الكارد للروت
        rootLayout.addView(cardLayout)
        setContentView(rootLayout)

        // --- منطق التحقق من التحديث ---
        UpdateChecker.checkForUpdate(this, object : UpdateChecker.UpdateListener {
            override fun onUpdateAvailable(version: String, url: String, changes: String) {
                updateButton.text = "Download Update ($version)"
                updateButton.background = getRoundedButtonDrawable("#2196F3") // أزرق
                updateButton.isEnabled = true
                updateButton.setOnClickListener {
                    startInternalDownload(url, version)
                }
                Toast.makeText(this@MainActivity, "Update Available!", Toast.LENGTH_SHORT).show()
            }

            override fun onNoUpdate() {
                updateButton.text = "Latest Version Installed"
                updateButton.background = getRoundedButtonDrawable("#4CAF50") // أخضر
                updateButton.isEnabled = false
            }

            override fun onError(error: String) {
                updateButton.text = "Check Failed"
                updateButton.isEnabled = true
                updateButton.setOnClickListener { recreate() } // إعادة المحاولة
            }
        })

        // --- تسجيل مستمع لانتهاء التحميل (الحل الصحيح للخطأ) ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    // --- دالة الرسم للأزرار الدائرية ---
    private fun getRoundedButtonDrawable(colorHex: String): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(colorHex))
            cornerRadius = 30f
        }
    }

    // --- منطق التحميل الداخلي ---
    private fun startInternalDownload(url: String, version: String) {
        try {
            // التحقق من صلاحية التثبيت (أندرويد 8+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!packageManager.canRequestPackageInstalls()) {
                    startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:$packageName")
                    })
                    Toast.makeText(this, "Please allow installation from this source", Toast.LENGTH_LONG).show()
                    return
                }
            }

            val fileName = "LinkifyAll_$version.apk"
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("Downloading LinkifyAll $version")
                .setDescription("Downloading update...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setMimeType("application/vnd.android.package-archive")

            val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = manager.enqueue(request)

            Toast.makeText(this, "Downloading started...", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Download Error: ${e.message}", Toast.LENGTH_SHORT).show()
            openUrl(url)
        }
    }

    // مستمع لانتهاء التحميل وبدء التثبيت
    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadId == id) {
                installApk(id)
            }
        }
    }

    private fun installApk(downloadId: Long) {
        try {
            val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = manager.getUriForDownloadedFile(downloadId)

            if (uri != null) {
                val installIntent = Intent(Intent.ACTION_VIEW)
                installIntent.setDataAndType(uri, "application/vnd.android.package-archive")
                installIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(installIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Install Failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(onDownloadComplete) } catch (e: Exception) {}
    }

    private fun openUrl(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) {}
    }

    private fun isModuleActive(): Boolean = false // Hook will change this
}