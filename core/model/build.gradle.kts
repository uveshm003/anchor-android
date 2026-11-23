plugins {
    alias(libs.plugins.anchor.jvm.library)
    alias(libs.plugins.kotlin.serialization)
}

// Domain types are @Serializable (format-agnostic core only) so the database and the engine-state snapshot can
// store them without a parallel set of DTOs.
dependencies {
    api(libs.kotlinx.serialization.core)

    testImplementation(libs.kotlinx.serialization.json)
}
