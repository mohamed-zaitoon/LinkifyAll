package com.mohamedzaitoon.linkifyall

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import java.io.File
import com.google.firebase.FirebaseApp // 👈 ضيف دي
class MainActivity : Activity() {

    // Default URLs
    private var currentGithubUrl = ""
    private var currentWebsiteUrl = ""

    private var downloadId: Long = -1
    private var downloadFileName: String = ""

    // Dialog Components
    private var progressDialog: AlertDialog? = null
    private var dialogProgressBar: ProgressBar? = null
    private var dialogPercentText: TextView? = null
    private var isDownloading = false

    // UI References
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var updateButton: Button
    private lateinit var githubButton: TextView
    private lateinit var websiteButton: TextView

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        // 1. SwipeRefreshLayout
        swipeRefreshLayout = SwipeRefreshLayout(this).apply {
            setColorSchemeColors(Color.parseColor("#2196F3"))
            setProgressBackgroundColorSchemeColor(Color.WHITE)
        }

        // 2. ScrollView
        val scrollView = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(Color.parseColor("#F2F4F8"))
        }

        // 3. Root Layout
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(60, 60, 60, 60)
        }

        // --- Card UI ---
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

        val titleView = TextView(this).apply {
            text = "LinkifyAll"
            textSize = 32f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.parseColor("#1A1A1A"))
            gravity = Gravity.CENTER
        }
        cardLayout.addView(titleView)

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

        val versionInfo = try { packageManager.getPackageInfo(packageName, 0).versionName } catch (e: Exception) { "?" }
        val versionView = TextView(this).apply {
            text = "Version $versionInfo"
            textSize = 16f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
        }
        cardLayout.addView(versionView)

        cardLayout.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(100, 2).apply { setMargins(0, 40, 0, 40) }
            setBackgroundColor(Color.LTGRAY)
        })

        updateButton = Button(this).apply {
            text = "Check for Updates"
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            background = getRoundedButtonDrawable("#BDBDBD")
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 140
            ).apply { setMargins(20, 10, 20, 30) }
            setOnClickListener { checkForUpdates() }
        }
        cardLayout.addView(updateButton)

        val linksLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 40, 0, 0)
        }

        fun createIconLink(emoji: String, label: String, initialUrl: String): TextView {
            val tv = TextView(this).apply {
                text = "$emoji $label"
                textSize = 14f
                setTextColor(Color.parseColor("#1976D2"))
                setPadding(30, 20, 30, 20)
                gravity = Gravity.CENTER
                setOnClickListener { openUrl(initialUrl) }
            }
            linksLayout.addView(tv)
            return tv
        }

        githubButton = createIconLink("🐙", "GitHub", currentGithubUrl)
        websiteButton = createIconLink("🌐", "Website", currentWebsiteUrl)

        cardLayout.addView(linksLayout)

        val devInfo = TextView(this).apply {
            text = "© Mohamed Zaitoon"
            textSize = 12f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 40, 0, 0)
        }
        cardLayout.addView(devInfo)

        rootLayout.addView(cardLayout)
        scrollView.addView(rootLayout)
        swipeRefreshLayout.addView(scrollView)
        setContentView(swipeRefreshLayout)

        swipeRefreshLayout.setOnRefreshListener { checkForUpdates() }
        checkForUpdates()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    private fun checkForUpdates() {
        if (!swipeRefreshLayout.isRefreshing) {
            updateButton.text = "Checking..."
            updateButton.isEnabled = false
        }
        UpdateChecker.checkForUpdate(this, object : UpdateChecker.UpdateListener {
            override fun onConfigFetched(result: UpdateChecker.ConfigResult) {
                swipeRefreshLayout.isRefreshing = false

                // Update Links
                currentGithubUrl = result.githubUrl
                currentWebsiteUrl = result.websiteUrl
                githubButton.setOnClickListener { openUrl(currentGithubUrl) }
                websiteButton.setOnClickListener { openUrl(currentWebsiteUrl) }

                // Check update
                if (result.updateAvailable) {
                    updateButton.text = "Download Update (${result.latestVersionName})"
                    updateButton.background = getRoundedButtonDrawable("#2196F3")
                    updateButton.isEnabled = true
                    updateButton.setOnClickListener {
                        startInternalDownload(result.downloadUrl, result.latestVersionName)
                    }
                    Toast.makeText(this@MainActivity, "Update Available: ${result.latestVersionName}", Toast.LENGTH_SHORT).show()
                } else {
                    updateButton.text = "Latest Version Installed"
                    updateButton.background = getRoundedButtonDrawable("#4CAF50")
                    updateButton.isEnabled = false
                }
            }

            override fun onError(error: String) {
                swipeRefreshLayout.isRefreshing = false
                updateButton.text = "Check Failed (Tap to Retry)"
                updateButton.background = getRoundedButtonDrawable("#F44336")
                updateButton.isEnabled = true
                updateButton.setOnClickListener { checkForUpdates() }
                Toast.makeText(this@MainActivity, "Error: $error", Toast.LENGTH_LONG).show()
            }
        })
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

            showProgressDialog()
            startDownloadWatcher(manager)

        } catch (e: Exception) {
            Toast.makeText(this, "Download Error: ${e.message}", Toast.LENGTH_SHORT).show()
            openUrl(url)
        }
    }

    private fun showProgressDialog() {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            gravity = Gravity.CENTER
        }

        val title = TextView(this).apply {
            text = "Downloading..."
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
        }
        dialogView.addView(title)

        dialogProgressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false
            max = 100
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        dialogView.addView(dialogProgressBar)

        dialogPercentText = TextView(this).apply {
            text = "0%"
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 0)
        }
        dialogView.addView(dialogPercentText)

        progressDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        progressDialog?.show()
    }

    private fun startDownloadWatcher(manager: DownloadManager) {
        isDownloading = true
        val handler = Handler(Looper.getMainLooper())

        val runnable = object : Runnable {
            override fun run() {
                if (!isDownloading) return

                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor: Cursor = manager.query(query)

                if (cursor.moveToFirst()) {
                    val statusCol = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    if (statusCol > -1) {
                        val status = cursor.getInt(statusCol)
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            isDownloading = false
                            progressDialog?.dismiss()
                            // Receiver will handle install
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            isDownloading = false
                            progressDialog?.dismiss()
                            Toast.makeText(this@MainActivity, "Download Failed", Toast.LENGTH_SHORT).show()
                        } else {
                            val bytesCol = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                            val totalCol = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                            if (bytesCol > -1 && totalCol > -1) {
                                val current = cursor.getInt(bytesCol)
                                val total = cursor.getInt(totalCol)
                                if (total > 0) {
                                    val progress = ((current * 100L) / total).toInt()
                                    dialogProgressBar?.progress = progress
                                    dialogPercentText?.text = "$progress%"
                                }
                            }
                            handler.postDelayed(this, 250)
                        }
                    }
                }
                cursor.close()
            }
        }
        handler.post(runnable)
    }

    private fun getRoundedButtonDrawable(colorHex: String): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor(colorHex))
            cornerRadius = 30f
        }
    }

    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadId == id) {
                isDownloading = false
                progressDialog?.dismiss()
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
        isDownloading = false
        try { unregisterReceiver(onDownloadComplete) } catch (e: Exception) {}
    }

    private fun openUrl(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) {}
    }

    private fun isModuleActive(): Boolean = false
}