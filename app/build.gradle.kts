import com.android.build.api.dsl.ApplicationExtension
import org.gradle.kotlin.dsl.configure
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

configure<ApplicationExtension> {
    namespace = "com.mohamedzaitoon.linkifyall"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.mohamedzaitoon.linkifyall"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()
    }

    // 👇 1. تحميل ملف إعدادات التوقيع
    val keystorePropertiesFile = listOf(
        rootProject.file("keystore.properties"),
        rootProject.file("key.properties")
    ).firstOrNull { it.exists() }
    val keystoreProperties = Properties()
    if (keystorePropertiesFile != null) {
        keystoreProperties.load(FileInputStream(keystorePropertiesFile))
    }

    // 👇 2. إعداد التوقيع (Signing Config)
    signingConfigs {
        create("release") {
            // Support both camelCase and snake_case keys so local files stay flexible
            val alias = (keystoreProperties["keyAlias"] ?: keystoreProperties["key_alias"]) as String?
            val keyPass = (keystoreProperties["keyPassword"] ?: keystoreProperties["key_password"]) as String?
            val storePath = (keystoreProperties["storeFile"] ?: keystoreProperties["store_file"]) as String?
            val storePass = (keystoreProperties["storePassword"] ?: keystoreProperties["store_password"]) as String?

            keyAlias = alias
            keyPassword = keyPass
            storeFile = storePath?.let { file(it) }
            storePassword = storePass
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // 👇 3. استخدام التوقيع الحقيقي هنا بدلاً من debug
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

dependencies {
    compileOnly(fileTree(mapOf(
        "dir" to "libs",
        "include" to listOf("*.jar")
    )))
    compileOnly(libs.xposed.api)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.swiperefreshlayout)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.config)
    implementation(libs.firebase.analytics)
}
