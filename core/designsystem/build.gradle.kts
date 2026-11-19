plugins {
    alias(libs.plugins.anchor.android.library.compose)
}

dependencies {
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
}

dependencies {
    testImplementation(libs.androidx.test.ext.junit)
}
