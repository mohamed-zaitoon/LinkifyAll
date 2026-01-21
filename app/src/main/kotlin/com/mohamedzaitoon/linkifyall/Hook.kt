package com.mohamedzaitoon.linkifyall

import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.TextView
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class Hook : IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {

        XposedHelpers.findAndHookMethod(
            TextView::class.java,
            "setText",
            CharSequence::class.java,
            TextView.BufferType::class.java,
            Boolean::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            object : XC_MethodHook() {

                override fun afterHookedMethod(param: MethodHookParam) {
                    val tv = param.thisObject as TextView
                    tv.autoLinkMask = Linkify.ALL
                    tv.linksClickable = true
                    tv.movementMethod = LinkMovementMethod.getInstance()
                }
            }
        )
    }
}