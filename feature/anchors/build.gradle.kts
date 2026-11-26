plugins {
    alias(libs.plugins.anchor.android.feature)
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.nfc)
    implementation(projects.core.qr)
}
