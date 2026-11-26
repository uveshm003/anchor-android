plugins {
    alias(libs.plugins.anchor.android.feature)
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.qr)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
}
