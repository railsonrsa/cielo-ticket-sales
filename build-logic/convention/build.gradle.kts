plugins {
    `kotlin-dsl`
}

group = "dev.railson.ticketsales.buildlogic"

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.compose.compiler.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("jvmLibrary") {
            id = libs.plugins.cielo.jvm.library.get().pluginId
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.cielo.android.library.get().pluginId
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = libs.plugins.cielo.android.compose.get().pluginId
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidFeature") {
            id = libs.plugins.cielo.android.feature.get().pluginId
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("androidApplication") {
            id = libs.plugins.cielo.android.application.get().pluginId
            implementationClass = "AndroidApplicationConventionPlugin"
        }
    }
}
