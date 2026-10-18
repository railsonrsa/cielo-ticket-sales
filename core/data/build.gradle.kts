plugins {
    alias(libs.plugins.cielo.android.library)
}

android {
    namespace = "dev.railson.ticketsales.core.data"
}

dependencies {
    implementation(projects.core.domain)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
}
