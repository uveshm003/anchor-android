plugins {
    alias(libs.plugins.anchor.android.library.compose)
}

dependencies {
    api(projects.core.common)
    api(testFixtures(projects.core.common))
    api(projects.core.model)

    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui.test.junit4)
    api(libs.androidx.test.espresso.core)
    api(libs.junit4)
    api(libs.kotlinx.coroutines.test)
    api(libs.robolectric)
    api(libs.roborazzi)
    api(libs.turbine)

    implementation(libs.androidx.activity.compose)
}
