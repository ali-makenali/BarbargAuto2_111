import org.gradle.api.JavaVersion

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
        versionCode = 5
        versionName = "2.2.1"
        // Supply through ~/.gradle/gradle.properties or CI secrets; never commit credentials.
        val utcCompany = providers.gradleProperty("UTCMS_COMPANY_CODE").orNull ?: ""
        val utcPassword = providers.gradleProperty("UTCMS_SERVICE_PASSWORD").orNull ?: ""
        buildConfigField("String", "UTCMS_COMPANY_CODE", "\\\"${utcCompany}\\\"")
        buildConfigField("String", "UTCMS_SERVICE_PASSWORD", "\\\"${utcPassword}\\\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug { isMinifyEnabled = false }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { buildConfig = true }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.google.android.material:material:1.12.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
