import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.withType

/**
 * Módulo de feature: só presentation. Enxerga `:core:domain` e `:core:ui` e nenhum outro módulo do projeto
 * (em especial, nenhuma outra feature). A regra quebra o build no momento em que a dependência é declarada.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("cielo.android.library")
        pluginManager.apply("cielo.android.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

        enforceFeatureDependencyRule()

        dependencies {
            add("implementation", project(":core:domain"))
            add("implementation", project(":core:ui"))

            add("implementation", platform(libs.library("koin-bom")))
            add("implementation", libs.library("koin-androidx-compose"))
            add("implementation", libs.library("androidx-navigation-compose"))
            add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
            add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
            add("implementation", libs.library("kotlinx-serialization-json"))

            add("testImplementation", testFixtures(project(":core:domain")))
            add("testImplementation", libs.library("kotlinx-coroutines-test"))
            add("testImplementation", libs.library("turbine"))
        }
    }

    private fun Project.enforceFeatureDependencyRule() {
        val featurePath = path
        configurations.configureEach {
            val configurationName = name
            dependencies.withType<ProjectDependency>().configureEach {
                // O AGP liga os source sets de teste ao próprio módulo.
                if (path != featurePath && path !in ALLOWED_PROJECT_DEPENDENCIES) {
                    throw GradleException(
                        "$featurePath cannot depend on $path ($configurationName): " +
                            "a feature module only sees ${ALLOWED_PROJECT_DEPENDENCIES.joinToString()}. " +
                            "Wire features together through navigation callbacks in :app.",
                    )
                }
            }
        }
    }

    private companion object {
        val ALLOWED_PROJECT_DEPENDENCIES = setOf(":core:domain", ":core:ui")
    }
}
