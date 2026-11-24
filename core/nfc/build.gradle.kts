plugins {
    alias(libs.plugins.anchor.android.library)
    alias(libs.plugins.anchor.hilt)
}

// Adapter: talks to the engine through its ports only (ADR 0003).
dependencies {
    implementation(projects.core.common)
    implementation(projects.core.engine)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)

    testImplementation(projects.core.testing)
    testImplementation(libs.truth)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.robolectric)
}
