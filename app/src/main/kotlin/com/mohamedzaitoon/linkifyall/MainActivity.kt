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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.io.File

class MainActivity : Activity() {

    private val GITHUB_URL = "https://github.com/mohamed-zaitoon/LinkifyAll"
    private val WEBSITE_URL = "https://mohamedzaitoon.com"

    private var downloadId: Long = -1
    private var downloadFileName: String = ""

    // عناصر الواجهة التي نحتاج لتحديثها
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var updateButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. إعداد SwipeRefreshLayout (الحاوية الرئيسية)
        swipeRefreshLayout = SwipeRefreshLayout(this).apply {
            setColorSchemeColors(Color.parseColor("#2196F3")) // لون دائرة التحميل
            setProgressBackgroundColorSchemeColor(Color.WHITE)
        }

        // 2. إعداد ScrollView (ضروري لعمل السحب بشكل سليم)
        val scrollView = ScrollView(this).apply {
            isFillViewport = true // يملأ الشاشة
            setBackgroundColor(Color.parseColor("#F2F4F8")) // خلفية رمادية
        }

        // 3. المحتوى الداخلي (Card Layout القديم)
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(60, 60, 60, 60)
        }

        // --- بناء البطاقة (Card) ---
        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(50, 80, 50, 80)
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 40f
                setStroke(2, Color.parseColor("#E0E0E0"))
            }
            elevation = 10f
        }

        // العنوان
        val titleView = TextView(this).apply {
            text = "LinkifyAll"
            textSize = 32f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#1A1A1A"))
            gravity = Gravity.CENTER
        }
        cardLayout.addView(titleView)

        // حالة الموديول
        val isActive = isModuleActive()
        val statusBadge = TextView(this).apply {
            text = if (isActive) "Active ●" else "Inactive ●"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor(if (isActive) "#2E7D32" else "#C62828"))
            gravity = Gravity.CENTER
            setPadding(40, 15, 40, 15)
            background = GradientDrawable().apply {
                setColor(Color.parseColor(if (isActive) "#E8F5E9" else "#FFEBEE"))
                cornerRadius = 50f
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 30, 0, 50) }
        }
        cardLayout.addView(statusBadge)

        // رقم الإصدار
        val versionInfo = try { packageManager.getPackageInfo(packageName, 0).versionName } catch (e: Exception) { "?" }
        val versionView = TextView(this).apply {
            text = "Version $versionInfo"
            textSize = 16f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
        }
        cardLayout.addView(versionView)

        // فاصل
        cardLayout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(100, 2).apply { setMargins(0, 40, 0, 40) }
            setBackgroundColor(Color.LTGRAY)
        })

        // زر التحديث (تعريف المتغير لاستخدامه لاحقاً)
        updateButton = Button(this).apply {
            text = "Check for Updates"
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            background = getRoundedButtonDrawable("#BDBDBD") // رمادي
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 140
            ).apply { setMargins(20, 10, 20, 30) }

            // عند الضغط، نقوم بالتحديث اليدوي
            setOnClickListener {
                checkForUpdates()
            }
        }
        cardLayout.addView(updateButton)

        // الروابط
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

        // حقوق المطور
        val devInfo = TextView(this).apply {
            text = "© Mohamed Zaitoon"
            textSize = 12f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 40, 0, 0)
        }
        cardLayout.addView(devInfo)

        // --- تجميع الهيكل ---
        rootLayout.addView(cardLayout) // إضافة البطاقة للـ Linear
        scrollView.addView(rootLayout) // إضافة الـ Linear للـ Scroll
        swipeRefreshLayout.addView(scrollView) // إضافة الـ Scroll للـ Swipe

        setContentView(swipeRefreshLayout)

        // --- إعداد منطق التحديث ---

        // 1. عند سحب الشاشة
        swipeRefreshLayout.setOnRefreshListener {
            checkForUpdates()
        }

        // 2. فحص تلقائي عند فتح التطبيق
        checkForUpdates()

        // --- تسجيل Receiver التحميل ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    // --- دالة التحقق من التحديث (منفصلة) ---
    private fun checkForUpdates() {
        // تحديث حالة الواجهة لتدل على التحميل
        if (!swipeRefreshLayout.isRefreshing) {
            updateButton.text = "Checking..."
            updateButton.isEnabled = false
        }

        UpdateChecker.checkForUpdate(this, object : UpdateChecker.UpdateListener {
            override fun onUpdateAvailable(version: String, url: String, changes: String) {
                // إيقاف دائرة التحميل
                swipeRefreshLayout.isRefreshing = false

                updateButton.text = "Download Update ($version)"
                updateButton.background = getRoundedButtonDrawable("#2196F3") // أزرق
                updateButton.isEnabled = true
                updateButton.setOnClickListener {
                    startInternalDownload(url, version)
                }

                Toast.makeText(this@MainActivity, "Update Available: $version", Toast.LENGTH_SHORT).show()
            }

            override fun onNoUpdate() {
                swipeRefreshLayout.isRefreshing = false

                updateButton.text = "Latest Version Installed"
                updateButton.background = getRoundedButtonDrawable("#4CAF50") // أخضر
                updateButton.isEnabled = false
            }

            override fun onError(error: String) {
                swipeRefreshLayout.isRefreshing = false

                updateButton.text = "Check Failed (Tap to Retry)"
                updateButton.background = getRoundedButtonDrawable("#F44336") // أحمر
                updateButton.isEnabled = true
                updateButton.setOnClickListener {
                    checkForUpdates()
                }
                // طباعة الخطأ للمساعدة في الديباج
                Toast.makeText(this@MainActivity, "Error: $error", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun getRoundedButtonDrawable(colorHex: String): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(colorHex))
            cornerRadius = 30f
        }
    }

    private fun startInternalDownload(url: String, version: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!packageManager.canRequestPackageInstalls()) {
                    startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:$packageName")
                    })
                    Toast.makeText(this, "Please allow permissions for fallback installation", Toast.LENGTH_LONG).show()
                    return
                }
            }

            downloadFileName = "LinkifyAll_$version.apk"
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), downloadFileName)
            if (file.exists()) file.delete()

            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("Downloading LinkifyAll $version")
                .setDescription("Downloading update...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, downloadFileName)
                .setMimeType("application/vnd.android.package-archive")

            val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = manager.enqueue(request)
            Toast.makeText(this, "Downloading started...", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Download Error: ${e.message}", Toast.LENGTH_SHORT).show()
            openUrl(url)
        }
    }

    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadId == id) {
                handleInstallation(id)
            }
        }
    }

    private fun handleInstallation(downloadId: Long) {
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), downloadFileName)
        if (file.exists()) {
            Toast.makeText(this, "Installing via Root...", Toast.LENGTH_SHORT).show()
            val success = installWithRoot(file.absolutePath)
            if (success) return
        }
        Toast.makeText(this, "Root install failed, trying standard...", Toast.LENGTH_SHORT).show()
        installStandard(downloadId)
    }

    private fun installWithRoot(path: String): Boolean {
        return try {
            val command = "pm install -r \"$path\""
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    private fun installStandard(downloadId: Long) {
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

    private fun isModuleActive(): Boolean = false
}