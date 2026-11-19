plugins {
    alias(libs.plugins.anchor.android.library)
    alias(libs.plugins.anchor.hilt)
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.engine)
}
