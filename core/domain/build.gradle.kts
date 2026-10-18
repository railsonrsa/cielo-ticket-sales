plugins {
    alias(libs.plugins.cielo.jvm.library)
    `java-test-fixtures`
}

dependencies {
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testFixturesImplementation(libs.kotlinx.coroutines.core)
}
