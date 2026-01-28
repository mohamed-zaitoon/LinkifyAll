package com.mohamedzaitoon.linkifyall

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    // رابط السورس كود
    private val GITHUB_URL = "https://github.com/mohamed-zaitoon/LinkifyAll"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- بناء الواجهة برمجياً (UI Construction) ---
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(50, 50, 50, 50)
            setBackgroundColor(Color.WHITE) // خلفية بيضاء
        }

        // 1. اسم التطبيق
        val titleView = TextView(this).apply {
            text = "LinkifyAll"
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
        }
        layout.addView(titleView)

        // 2. رقم الإصدار (ديناميكي)
        val versionInfo = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) { "Unknown" }

        val versionView = TextView(this).apply {
            text = "Version: $versionInfo"
            textSize = 16f
            setTextColor(Color.DKGRAY)
            setPadding(0, 20, 0, 20)
            gravity = Gravity.CENTER
        }
        layout.addView(versionView)

        // 3. حالة الموديول (Active/Inactive)
        val statusView = TextView(this).apply {
            text = if (isModuleActive()) "✅ Module is Active" else "❌ Module is NOT Active"
            textSize = 18f
            setTextColor(if (isModuleActive()) Color.parseColor("#4CAF50") else Color.RED)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 30, 0, 30)
            gravity = Gravity.CENTER
        }
        layout.addView(statusView)

        if (!isModuleActive()) {
            val hintView = TextView(this).apply {
                text = "Enable in Xposed Manager & Reboot"
                textSize = 14f
                setTextColor(Color.GRAY)
                gravity = Gravity.CENTER
            }
            layout.addView(hintView)
        }

        // 4. رابط السورس كود (Github)
        val githubLink = TextView(this).apply {
            text = "View Source Code on GitHub"
            textSize = 16f
            setTextColor(Color.BLUE)
            paintFlags = paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG // وضع خط تحت النص
            setPadding(0, 50, 0, 0)
            gravity = Gravity.CENTER

            setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL))
                startActivity(intent)
            }
        }
        layout.addView(githubLink)

        setContentView(layout)

        // --- التحقق التلقائي من التحديث ---
        // يتم في الخلفية بمجرد فتح التطبيق
        UpdateChecker.checkForUpdate(this, isManualCheck = false)
    }

    // هذه الدالة ترجع دائماً false، ولكن الـ Hook سيقوم بتغييرها لـ true إذا كان الموديول يعمل
    private fun isModuleActive(): Boolean {
        return false
    }
}