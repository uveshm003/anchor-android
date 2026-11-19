plugins {
    alias(libs.plugins.anchor.android.library.compose)
    alias(libs.plugins.anchor.hilt)
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.designsystem)
}
