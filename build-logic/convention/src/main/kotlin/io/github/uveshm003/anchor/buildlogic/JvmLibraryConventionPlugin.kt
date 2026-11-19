package io.github.uveshm003.anchor.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Pure Kotlin/JVM module: no Android dependencies allowed. Coverage gate: >= 80% (engine raises it to 90%). */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            pluginManager.apply("com.android.lint")
            configureKotlinJvm()
            dependencies {
                add("testImplementation", libs.lib("junit4"))
                add("testImplementation", libs.lib("kotlin-test"))
                add("testImplementation", libs.lib("kotlinx-coroutines-test"))
                add("testImplementation", libs.lib("turbine"))
                add("testImplementation", libs.lib("truth"))
            }
            configureKover()
            koverMinimumCoverage(PURE_KOTLIN_MIN_COVERAGE)
            registerModuleCheck("test", "lint", "koverVerify")
        }
    }
}

private const val PURE_KOTLIN_MIN_COVERAGE = 80
