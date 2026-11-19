plugins {
    alias(libs.plugins.anchor.android.library)
}

dependencies {
    api(projects.core.common)
    api(projects.core.model)
    api(libs.junit4)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
}
