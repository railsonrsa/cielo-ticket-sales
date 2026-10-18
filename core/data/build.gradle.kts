plugins {
    alias(libs.plugins.cielo.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "dev.railson.ticketsales.core.data"

    testOptions {
        // O teste do seed lê o events.json de verdade.
        unitTests.isIncludeAndroidResources = true
    }

    // O teste de migração abre os esquemas exportados.
    sourceSets.getByName("test").assets.srcDir("$projectDir/schemas")
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(projects.core.domain)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.room.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
