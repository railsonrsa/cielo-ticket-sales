import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.versionInt(alias: String): Int =
    findVersion(alias).get().requiredVersion.toInt()

internal fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).get()

internal fun Project.configureKotlinToolchain() {
    extensions.getByType<KotlinProjectExtension>().jvmToolchain(libs.versionInt("jvmToolchain"))
}

internal fun Project.configureAndroid(android: CommonExtension<*, *, *, *, *, *>) {
    android.apply {
        compileSdk = libs.versionInt("compileSdk")
        defaultConfig {
            minSdk = libs.versionInt("minSdk")
        }
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        lint {
            targetSdk = libs.versionInt("targetSdk")
            abortOnError = true
        }
    }
    configureKotlinToolchain()
    configureUnitTests()
}

internal fun Project.configureUnitTests() {
    dependencies {
        add("testImplementation", libs.library("junit"))
        add("testImplementation", libs.library("truth"))
    }
}
