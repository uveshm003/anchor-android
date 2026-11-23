plugins {
    alias(libs.plugins.anchor.android.library)
    alias(libs.plugins.anchor.android.room)
    alias(libs.plugins.anchor.hilt)
}

dependencies {
    api(projects.core.model)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(projects.core.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.robolectric)
}
