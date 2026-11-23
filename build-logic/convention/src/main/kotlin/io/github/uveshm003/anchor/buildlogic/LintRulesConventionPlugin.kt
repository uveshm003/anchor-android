package io.github.uveshm003.anchor.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** The `:lint-rules` module: custom Android Lint checks, run by every module via `lintChecks` (see [addLintRules]). */
class LintRulesConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            configureKotlinJvm()
            dependencies {
                // Lint supplies its own API and Kotlin stdlib at runtime.
                add("compileOnly", libs.lib("android-lint-api"))
                add("testImplementation", libs.lib("android-lint-api"))
                add("testImplementation", libs.lib("android-lint-tests"))
                add("testImplementation", libs.lib("junit4"))
                add("testImplementation", libs.lib("truth"))
            }
            configureKover()
            koverMinimumCoverage(LINT_RULES_MIN_COVERAGE)
            registerModuleCheck("test", "koverVerify")
        }
    }
}

private const val LINT_RULES_MIN_COVERAGE = 80
