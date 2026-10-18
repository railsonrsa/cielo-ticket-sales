plugins {
    alias(libs.plugins.cielo.android.library)
}

android {
    namespace = "dev.railson.ticketsales.payment.fake"
}

dependencies {
    implementation(projects.core.domain)
}
