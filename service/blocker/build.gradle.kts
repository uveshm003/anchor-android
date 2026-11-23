plugins {
    alias(libs.plugins.anchor.android.library.compose)
    alias(libs.plugins.anchor.hilt)
}

// Adapter: depends on the engine ports only, never on core:data (ADR 0003).
dependencies {
    implementation(projects.core.common)
    implementation(projects.core.engine)
    implementation(projects.core.designsystem)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(projects.core.testing)
    testImplementation(projects.core.screenshotTesting)
    testImplementation(libs.truth)
}
