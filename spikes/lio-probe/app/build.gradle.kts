plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dev.railson.lioprobe"
    compileSdk = 34
    defaultConfig {
        applicationId = "dev.railson.lioprobe"
        minSdk = 25
        targetSdk = 29 // recomendado pela Cielo
        versionCode = 1
        versionName = "1"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    lint { checkReleaseBuilds = false; abortOnError = false }
}
