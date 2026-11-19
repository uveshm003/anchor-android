package io.github.uveshm003.anchor.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Hilt via KSP. Modules whose tests need Hilt add `hilt-android-testing` + `kspTest(hilt-compiler)` themselves,
 * so modules without Hilt tests do not get generated test sources.
 */
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")
            pluginManager.apply("com.google.dagger.hilt.android")
            dependencies {
                add("implementation", libs.lib("hilt-android"))
                add("ksp", libs.lib("hilt-compiler"))
            }
        }
    }
}
