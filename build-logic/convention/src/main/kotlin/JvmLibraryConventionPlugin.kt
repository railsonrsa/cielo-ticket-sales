import org.gradle.api.Plugin
import org.gradle.api.Project

/** Kotlin puro, sem Android. Usado pelo `:core:domain`. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")
        pluginManager.apply("com.android.lint")
        configureKotlinToolchain()
        configureUnitTests()
    }
}
