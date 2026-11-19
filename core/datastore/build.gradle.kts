plugins {
    alias(libs.plugins.anchor.android.library)
    alias(libs.plugins.anchor.hilt)
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)
}
