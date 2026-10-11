plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dev.codekey"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.codekey"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
