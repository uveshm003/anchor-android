plugins {
    alias(libs.plugins.anchor.jvm.library)
    alias(libs.plugins.kotlin.serialization)
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
    // Pure rules: only the domain model (ADR 0003). No Android, coroutines or I/O.
    api(projects.core.model)

    testImplementation(libs.kotlinx.serialization.json)
}
