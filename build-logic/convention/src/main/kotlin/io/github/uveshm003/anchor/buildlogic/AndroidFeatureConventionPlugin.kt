package io.github.uveshm003.anchor.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * A feature = screens + ViewModels + a `@Serializable` route and `NavGraphBuilder` entry for `app` to wire.
 * Depends on the design system and shared UI, never on another feature.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("anchor.android.library.compose")
            pluginManager.apply("anchor.hilt")
            // Type-safe navigation: each feature exposes a @Serializable route.
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
            dependencies {
                add("implementation", project(":core:designsystem"))
                add("implementation", project(":core:ui"))
                add("implementation", libs.lib("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.lib("androidx-lifecycle-viewmodel-compose"))
                add("implementation", libs.lib("androidx-hilt-lifecycle-viewmodel-compose"))
                add("implementation", libs.lib("androidx-navigation-compose"))
                add("implementation", libs.lib("kotlinx-serialization-json"))
                add("testImplementation", project(":core:testing"))
                add("testImplementation", project(":core:screenshot-testing"))
            }
        }
    }
}
