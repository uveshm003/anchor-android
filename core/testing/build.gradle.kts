plugins {
    alias(libs.plugins.anchor.android.library)
}

// Lightweight test support for any module: coroutines, Turbine, TestClock and fakes. Screenshot tooling
// (Robolectric/Roborazzi) is deliberately not here, so unit-test-only modules don't inherit it.
dependencies {
    api(projects.core.common)
    api(testFixtures(projects.core.common))
    api(projects.core.model)
    api(projects.core.data)

    api(libs.junit4)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)

    testImplementation(libs.truth)
}
