plugins {
    alias(libs.plugins.anchor.android.library)
    alias(libs.plugins.anchor.hilt)
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.engine)

    testImplementation(projects.core.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.truth)
}
