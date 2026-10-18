plugins {
    alias(libs.plugins.cielo.android.library)
}

android {
    namespace = "dev.railson.ticketsales.payment.cielolio"
}

dependencies {
    implementation(projects.core.domain)
}
