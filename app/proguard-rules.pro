# --- Basic Settings ---
# Keep line numbers and source file names (Crucial for debugging crashes)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Kotlin Settings ---
# Prevent obfuscation of Kotlin metadata (Essential for Coroutines)
-keep class kotlin.Metadata { *; }
-keepattributes RuntimeVisibleAnnotations
-keepattributes *Annotation*

# --- Xposed Settings (Most Important) ---
# 1. Keep Xposed library classes
-keep class de.robv.android.xposed.** { *; }

# 2. Keep any class implementing IXposedHookLoadPackage
-keep class * implements de.robv.android.xposed.IXposedHookLoadPackage {
    public void handleLoadPackage(de.robv.android.xposed.callbacks.XC_LoadPackage$LoadPackageParam);
}

# 3. Keep all classes in your package (Safe for Xposed modules)
-keep class com.mohamedzaitoon.linkifyall.** { *; }

# 4. Keep hook methods
-keepclassmembers class ** {
    public void handleLoadPackage(...);
}

# --- Additional Settings ---
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application

# --- Firebase specific (Optional, R8 usually handles this, but safe to add) ---
-keep class com.google.firebase.** { *; }