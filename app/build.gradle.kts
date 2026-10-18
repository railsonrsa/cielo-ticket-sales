plugins {
    alias(libs.plugins.cielo.android.application)
    alias(libs.plugins.cielo.android.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.railson.ticketsales"

    defaultConfig {
        applicationId = "dev.railson.ticketsales"
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.data)
    implementation(projects.core.ui)
    implementation(projects.feature.catalog)
    implementation(projects.feature.checkout)
    implementation(projects.feature.purchases)
    // A Cielo de verdade entra no #17, que decide como o build escolhe entre as duas.
    implementation(projects.payment.fake)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)

    testImplementation(libs.koin.test)
    testImplementation(libs.koin.test.junit4)
}
