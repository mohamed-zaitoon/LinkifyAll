package com.mohamedzaitoon.linkifyall

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement // لازم تعمل import لدي
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.regex.Pattern

class Hook : IXposedHookLoadPackage {

    private val urlPattern = Pattern.compile(
        "((?:http|https)://\\S+|www\\.\\S+|[a-zA-Z0-9.-]+\\.(?:com|net|org|io|gov|edu|me|xyz|info)\\S*)",
        Pattern.CASE_INSENSITIVE
    )
    private var lastCustomViewOpenAt = 0L

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {

        // 1. الجزء الجديد: تفعيل الحالة داخل تطبيقنا ليصبح Active ✅
        if (lpparam.packageName == "com.mohamedzaitoon.linkifyall") {
            XposedHelpers.findAndHookMethod(
                "com.mohamedzaitoon.linkifyall.MainActivity",
                lpparam.classLoader,
                "isModuleActive",
                object : XC_MethodReplacement() {
                    override fun replaceHookedMethod(param: MethodHookParam): Any {
                        return true // إرجاع true ليتحول اللون للأخضر
                    }
                }
            )
        }

        // تجاهل تطبيقات النظام الحساسة لمنع التعليق
        if (lpparam.packageName == "android" || lpparam.packageName == "com.android.systemui") return

        // 2. كود اللينكات الأساسي (زي ما هو)
        XposedHelpers.findAndHookMethod(
            TextView::class.java,
            "setText",
            CharSequence::class.java,
            TextView.BufferType::class.java,
            Boolean::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val originalText = param.args[0] as? CharSequence ?: return
                        if (originalText.isEmpty() || originalText.length > 2000) return

                        val textStr = originalText.toString()
                        if (!textStr.contains(".") && !textStr.contains("http")) return

                        val matcher = urlPattern.matcher(textStr)
                        if (matcher.find()) {
                            val spannable = if (originalText is Spannable) originalText else SpannableString(originalText)
                            var modified = false

                            matcher.reset()
                            while (matcher.find()) {
                                modified = true
                                spannable.setSpan(ForegroundColorSpan(Color.parseColor("#2196F3")), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                            }

                            if (modified) {
                                param.args[0] = spannable
                                param.args[1] = TextView.BufferType.SPANNABLE
                            }
                        }
                    } catch (e: Throwable) { }
                }
            }
        )

        XposedHelpers.findAndHookMethod(
            TextView::class.java,
            "onTouchEvent",
            MotionEvent::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val tv = param.thisObject as TextView
                        val event = param.args[0] as MotionEvent
                        val action = event.action

                        if (action == MotionEvent.ACTION_UP) {
                            val text = tv.text
                            if (text is Spannable) {
                                var x = event.x.toInt()
                                var y = event.y.toInt()

                                x -= tv.totalPaddingLeft
                                y -= tv.totalPaddingTop
                                x += tv.scrollX
                                y += tv.scrollY

                                val layout = tv.layout ?: return
                                val line = layout.getLineForVertical(y)

                                if (x < layout.getLineLeft(line) || x > layout.getLineRight(line)) return

                                val off = layout.getOffsetForHorizontal(line, x.toFloat())

                                val matcher = urlPattern.matcher(text.toString())
                                while (matcher.find()) {
                                    if (off >= matcher.start() && off <= matcher.end()) {
                                        val url = cleanMatchedUrl(matcher.group())
                                        openUrl(tv, url)

                                        param.setResult(true)
                                        return
                                    }
                                }
                            }
                        }
                        else if (action == MotionEvent.ACTION_DOWN) {
                            val text = tv.text
                            if (text is Spannable) {
                                var x = event.x.toInt(); var y = event.y.toInt()
                                x -= tv.totalPaddingLeft; y -= tv.totalPaddingTop; x += tv.scrollX; y += tv.scrollY
                                val layout = tv.layout ?: return
                                val line = layout.getLineForVertical(y)
                                val off = layout.getOffsetForHorizontal(line, x.toFloat())

                                val matcher = urlPattern.matcher(text.toString())
                                while (matcher.find()) {
                                    if (off >= matcher.start() && off <= matcher.end()) {
                                        tv.parent?.requestDisallowInterceptTouchEvent(true)
                                        param.setResult(true)
                                        return
                                    }
                                }
                            }
                        }

                    } catch (e: Throwable) {
                        XposedBridge.log(e)
                    }
                }
            }
        )

        hookCustomViewTouches(View::class.java)
        hookCustomViewTouches(ViewGroup::class.java)
    }

    private fun hookCustomViewTouches(viewClass: Class<*>) {
        XposedHelpers.findAndHookMethod(
            viewClass,
            "dispatchTouchEvent",
            MotionEvent::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    try {
                        val view = param.thisObject as? View ?: return
                        if (view is TextView) return

                        val event = param.args[0] as? MotionEvent ?: return
                        if (event.action != MotionEvent.ACTION_UP) return

                        val now = System.currentTimeMillis()
                        if (now - lastCustomViewOpenAt < 700L) return

                        val url = findUrlFromAccessibilityAt(view, event.rawX.toInt(), event.rawY.toInt()) ?: return
                        openUrl(view, url)
                        lastCustomViewOpenAt = now
                        param.setResult(true)
                    } catch (e: Throwable) {
                        XposedBridge.log(e)
                    }
                }
            }
        )
    }

    private fun findUrlFromAccessibilityAt(view: View, rawX: Int, rawY: Int): String? {
        val root = view.rootView?.createAccessibilityNodeInfo()
            ?: view.createAccessibilityNodeInfo()
            ?: return null

        return try {
            findUrlInNode(root, rawX, rawY, 0)
        } finally {
            try {
                root.recycle()
            } catch (_: Throwable) {
            }
        }
    }

    private fun findUrlInNode(node: AccessibilityNodeInfo, rawX: Int, rawY: Int, depth: Int): String? {
        if (depth > 40) return null

        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (!bounds.isEmpty && !bounds.contains(rawX, rawY)) return null

        for (i in 0 until node.childCount) {
            val child = try {
                node.getChild(i)
            } catch (_: Throwable) {
                null
            } ?: continue

            try {
                val childUrl = findUrlInNode(child, rawX, rawY, depth + 1)
                if (childUrl != null) return childUrl
            } finally {
                try {
                    child.recycle()
                } catch (_: Throwable) {
                }
            }
        }

        val text = buildString {
            node.text?.let { append(it) }
            if (isNotEmpty()) append(' ')
            node.contentDescription?.let { append(it) }
        }

        if (text.isBlank() || (!text.contains(".") && !text.contains("http", true))) return null
        val matcher = urlPattern.matcher(text)
        return if (matcher.find()) cleanMatchedUrl(matcher.group()) else null
    }

    private fun openUrl(view: View, url: String) {
        val finalUrl = if (url.startsWith("http", true)) url else "http://$url"
        val uri = Uri.parse(finalUrl)
        val sourcePackage = view.context.packageName
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val externalIntent = findExternalHandlerIntent(view, intent, sourcePackage)
        if (externalIntent != null) {
            view.context.startActivity(externalIntent)
            return
        }

        view.context.startActivity(createExternalChooserIntent(view, intent, sourcePackage))
    }

    private fun cleanMatchedUrl(url: String): String {
        return url.trim().trimEnd('.', ',', ';', ':', '!', '?', ')', ']', '}')
    }

    private fun findExternalHandlerIntent(view: View, baseIntent: Intent, sourcePackage: String): Intent? {
        val packageManager = view.context.packageManager
        val handlers = try {
            packageManager.queryIntentActivities(baseIntent, PackageManager.MATCH_DEFAULT_ONLY)
        } catch (_: Throwable) {
            return null
        }

        val externalHandlers = handlers.filter { it.activityInfo?.packageName != sourcePackage }
        val preferred = externalHandlers.firstOrNull { info ->
            val packageName = info.activityInfo?.packageName ?: return@firstOrNull false
            packageName.contains("chrome", ignoreCase = true) ||
                packageName.contains("browser", ignoreCase = true) ||
                packageName.contains("firefox", ignoreCase = true) ||
                packageName.contains("edge", ignoreCase = true) ||
                packageName.contains("brave", ignoreCase = true) ||
                packageName.contains("opera", ignoreCase = true) ||
                packageName.contains("duckduckgo", ignoreCase = true)
        } ?: externalHandlers.firstOrNull()

        val activityInfo = preferred?.activityInfo ?: return null
        return Intent(baseIntent).apply {
            component = ComponentName(activityInfo.packageName, activityInfo.name)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    private fun createExternalChooserIntent(view: View, baseIntent: Intent, sourcePackage: String): Intent {
        val packageManager = view.context.packageManager
        val excludedComponents = try {
            packageManager.queryIntentActivities(baseIntent, PackageManager.MATCH_DEFAULT_ONLY)
                .mapNotNull { info ->
                    val activityInfo = info.activityInfo ?: return@mapNotNull null
                    if (activityInfo.packageName == sourcePackage) {
                        ComponentName(activityInfo.packageName, activityInfo.name)
                    } else {
                        null
                    }
                }
                .toTypedArray()
        } catch (_: Throwable) {
            emptyArray()
        }

        return Intent.createChooser(baseIntent, "Open link").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            if (excludedComponents.isNotEmpty()) {
                putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, excludedComponents)
            }
        }
    }
}
