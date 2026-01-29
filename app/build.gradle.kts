import com.android.build.api.dsl.ApplicationExtension
import org.gradle.kotlin.dsl.configure

plugins {
    alias(libs.plugins.android.application)
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

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Build Types Configuration
    buildTypes {
        getByName("release") {
            // Enable code shrinking and obfuscation (ProGuard/R8)
            isMinifyEnabled = true

            // Enable resource shrinking to remove unused resources
            isShrinkResources = true

            // ProGuard rules files (Default Android optimize rules + your local rules)
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Use debug keys for signing (Useful for testing release builds without a keystore)
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    compileOnly(fileTree(mapOf(
        "dir" to "libs",
        "include" to listOf("*.jar")
    )))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.swiperefreshlayout)
}