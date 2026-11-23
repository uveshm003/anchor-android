plugins {
    alias(libs.plugins.anchor.android.library.compose)
}

dependencies {
    api(projects.core.designsystem)
    api(projects.core.model)
    implementation(libs.androidx.core.ktx)
}
