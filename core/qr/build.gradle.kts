plugins {
    alias(libs.plugins.anchor.android.library)
    alias(libs.plugins.anchor.hilt)
}

// Adapter: offline QR codes (ZXing core, no INTERNET; ADR 0009) and CameraX scanning.
dependencies {
    implementation(projects.core.common)
    implementation(projects.core.engine)
    api(libs.zxing.core)
    api(libs.androidx.camera.camera2)
    api(libs.androidx.camera.lifecycle)
    api(libs.androidx.camera.view)
    implementation(libs.androidx.core.ktx)

    testImplementation(projects.core.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.truth)
}
