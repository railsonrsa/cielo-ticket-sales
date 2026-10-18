plugins {
    alias(libs.plugins.cielo.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.railson.ticketsales.payment.cielolio"

    testOptions {
        // O teste da LioCallbackActivity precisa do manifest do módulo e do seed do :core:data.
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(projects.core.domain)
    implementation(libs.androidx.activity)
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)

    testImplementation(projects.core.data)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
