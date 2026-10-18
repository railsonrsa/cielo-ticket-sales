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

    // Qual maquininha entra no APK. Cada flavor monta o seu `paymentModule` em src/<flavor>.
    flavorDimensions += "terminal"
    productFlavors {
        // A FakePaymentTerminal: demonstra o app sem o emulador da Cielo. Instala ao lado do cielo.
        create("demo") {
            dimension = "terminal"
            applicationIdSuffix = ".demo"
        }
        // A Cielo LIO de verdade, por Deep Link.
        create("cielo") {
            dimension = "terminal"
        }
    }
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.data)
    implementation(projects.core.ui)
    implementation(projects.feature.catalog)
    implementation(projects.feature.checkout)
    implementation(projects.feature.purchases)
    "demoImplementation"(projects.payment.fake)
    "cieloImplementation"(projects.payment.cieloLio)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)

    testImplementation(libs.koin.test)
    testImplementation(libs.koin.test.junit4)
}
