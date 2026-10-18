plugins {
    alias(libs.plugins.cielo.android.library)
    alias(libs.plugins.cielo.android.compose)
}

android {
    namespace = "dev.railson.ticketsales.payment.fake"
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.ui)

    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
}
