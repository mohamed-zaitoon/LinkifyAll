package com.mohamedzaitoon.linkifyall

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.MotionEvent
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
                                spannable.setSpan(ForegroundColorSpan(Color.CYAN), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
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
                                        val url = matcher.group()
                                        val finalUrl = if (url.startsWith("http", true)) url else "http://$url"

                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        tv.context.startActivity(intent)

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
    }
}