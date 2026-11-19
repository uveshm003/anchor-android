plugins {
    alias(libs.plugins.anchor.android.library.compose)
}

android {
    namespace = "io.github.uveshm003.anchor.core.screenshottesting"
}

// Robolectric + Roborazzi screenshot tooling. Only test source sets depend on this module.
dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui.test.junit4)
    api(libs.androidx.test.espresso.core)
    api(libs.androidx.test.ext.junit)
    api(libs.junit4)
    api(libs.robolectric)
    api(libs.roborazzi)

    implementation(libs.androidx.activity.compose)
}
