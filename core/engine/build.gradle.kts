plugins {
    alias(libs.plugins.anchor.jvm.library)
}

// The mode engine is the heart of the app: held to a stricter gate than other modules.
kover {
    reports {
        verify {
            rule("Engine line coverage") {
                minBound(90)
            }
        }
    }
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)
}
