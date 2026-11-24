plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ir.barbarg.auto"
    compileSdk = 35

    defaultConfig {
        applicationId = "ir.barbarg.auto"
        minSdk = 23
        targetSdk = 35
        versionCode = 3
        versionName = "2.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
