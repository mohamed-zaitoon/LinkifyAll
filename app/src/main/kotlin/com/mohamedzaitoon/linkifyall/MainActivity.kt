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

    private var downloadId: Long = -1
    private var downloadFileName: String = "" // لحفظ اسم الملف لاستخدامه في الروت

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- Root Layout ---
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#F2F4F8"))
            setPadding(60, 60, 60, 60)
        }

        // --- Card Container ---
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

        // 1. Title
        val titleView = TextView(this).apply {
            text = "LinkifyAll"
            textSize = 32f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#1A1A1A"))
            gravity = Gravity.CENTER
        }
        cardLayout.addView(titleView)

        // 2. Module Status Badge
        val isActive = isModuleActive()
        val statusBgColor = if (isActive) "#E8F5E9" else "#FFEBEE"
        val statusTxtColor = if (isActive) "#2E7D32" else "#C62828"

        val statusBadge = TextView(this).apply {
            text = if (isActive) "Active ●" else "Inactive ●"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor(statusTxtColor))
            gravity = Gravity.CENTER
            setPadding(40, 15, 40, 15)
            background = GradientDrawable().apply {
                setColor(Color.parseColor(statusBgColor))
                cornerRadius = 50f
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 30, 0, 50) }
        }
        cardLayout.addView(statusBadge)

        // 3. Version Info
        val versionInfo = try { packageManager.getPackageInfo(packageName, 0).versionName } catch (e: Exception) { "?" }
        val versionView = TextView(this).apply {
            text = "Version $versionInfo"
            textSize = 16f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
        }
        cardLayout.addView(versionView)

        // Separator
        cardLayout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(100, 2).apply { setMargins(0, 40, 0, 40) }
            setBackgroundColor(Color.LTGRAY)
        })

        // 4. Update Button
        val updateButton = Button(this).apply {
            text = "Checking for updates..."
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            isEnabled = false
            background = getRoundedButtonDrawable("#BDBDBD")
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 140
            ).apply { setMargins(20, 10, 20, 30) }
        }
        cardLayout.addView(updateButton)

        // 5. Links
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

        // 6. Dev Info
        val devInfo = TextView(this).apply {
            text = "© Mohamed Zaitoon"
            textSize = 12f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 40, 0, 0)
        }
        cardLayout.addView(devInfo)

        rootLayout.addView(cardLayout)
        setContentView(rootLayout)

        // --- Update Logic ---
        UpdateChecker.checkForUpdate(this, object : UpdateChecker.UpdateListener {
            override fun onUpdateAvailable(version: String, url: String, changes: String) {
                updateButton.text = "Download Update ($version)"
                updateButton.background = getRoundedButtonDrawable("#2196F3")
                updateButton.isEnabled = true
                updateButton.setOnClickListener {
                    startInternalDownload(url, version)
                }
                Toast.makeText(this@MainActivity, "Update Available!", Toast.LENGTH_SHORT).show()
            }

            override fun onNoUpdate() {
                updateButton.text = "Latest Version Installed"
                updateButton.background = getRoundedButtonDrawable("#4CAF50")
                updateButton.isEnabled = false
            }

            override fun onError(error: String) {
                updateButton.text = "Check Failed"
                updateButton.isEnabled = true
                updateButton.setOnClickListener { recreate() }
            }
        })

        // --- Receiver Registration ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    private fun getRoundedButtonDrawable(colorHex: String): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(colorHex))
            cornerRadius = 30f
        }
    }

    private fun startInternalDownload(url: String, version: String) {
        try {
            // طلب إذن التثبيت العادي كاحتياطي
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!packageManager.canRequestPackageInstalls()) {
                    startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:$packageName")
                    })
                    Toast.makeText(this, "Please allow permissions for fallback installation", Toast.LENGTH_LONG).show()
                    return
                }
            }

            downloadFileName = "LinkifyAll_$version.apk" // حفظ الاسم

            // حذف الملف القديم إن وجد لتجنب مشاكل التسمية
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
        // 1. محاولة التثبيت بالروت أولاً
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), downloadFileName)

        if (file.exists()) {
            Toast.makeText(this, "Installing via Root...", Toast.LENGTH_SHORT).show()
            val success = installWithRoot(file.absolutePath)

            if (success) {
                return // تم التثبيت بنجاح، لا داعي لإكمال الكود
            }
        }

        // 2. إذا فشل الروت، نستخدم الطريقة العادية
        Toast.makeText(this, "Root install failed, trying standard...", Toast.LENGTH_SHORT).show()
        installStandard(downloadId)
    }

    // 🔥 دالة التثبيت بالروت
    private fun installWithRoot(path: String): Boolean {
        return try {
            // الأمر: su -c "pm install -r /path/to/apk"
            // -r تعني reinstall (تحديث) والحفاظ على البيانات
            val command = "pm install -r \"$path\""
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val exitCode = process.waitFor()

            // إذا كان كود الخروج 0 يعني نجح
            exitCode == 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // التثبيت العادي (Fallback)
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