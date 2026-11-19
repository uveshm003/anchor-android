package io.github.uveshm003.anchor.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** A feature = screens + ViewModels. Depends on the design system and shared UI, never on another feature. */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("anchor.android.library.compose")
            pluginManager.apply("anchor.hilt")
            dependencies {
                add("implementation", project(":core:designsystem"))
                add("implementation", project(":core:ui"))
                add("implementation", libs.lib("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.lib("androidx-lifecycle-viewmodel-compose"))
                add("implementation", libs.lib("androidx-hilt-lifecycle-viewmodel-compose"))
                add("testImplementation", project(":core:testing"))
            }
        }
    }
}
