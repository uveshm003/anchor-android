plugins {
    alias(libs.plugins.anchor.jvm.library)
    alias(libs.plugins.anchor.hilt)
    `java-test-fixtures`
}

dependencies {
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)
}
