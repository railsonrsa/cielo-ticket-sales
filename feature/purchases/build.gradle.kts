plugins {
    alias(libs.plugins.cielo.android.feature)
}

android {
    namespace = "dev.railson.ticketsales.feature.purchases"
}

dependencies {
    // Só o encoder do QR, em Java puro: não depende de Google Play Services, que o terminal não tem.
    implementation(libs.zxing.core)
}
