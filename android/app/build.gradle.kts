plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.arama.app"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.arama.app"
        minSdk = 26
        targetSdk = 35
        versionCode = (project.findProperty("versionCode") as String?)?.toInt() ?: 1
        versionName = (project.findProperty("versionName") as String?) ?: "0.1.0"
        val apiBaseUrl = (project.findProperty("apiBaseUrl") as String?)?.trim()?.trimEnd('/') ?: ""
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    signingConfigs {
        create("release") {
            val keystore = file("release-keystore.p12")
            storeFile = keystore
            storeType = "PKCS12"
            storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("ANDROID_KEY_ALIAS")
            keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
}
