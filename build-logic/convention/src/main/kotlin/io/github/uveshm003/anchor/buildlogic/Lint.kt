package io.github.uveshm003.anchor.buildlogic

import com.android.build.api.dsl.Lint
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** Lint errors fail the build. No baselines (see CONTRIBUTING.md). AGP 9 always writes all report formats. */

internal fun Project.configureLint(lint: Lint) = with(lint) {
    abortOnError = true
    checkDependencies = false
    checkReleaseBuilds = false
    explainIssues = false
    absolutePaths = false
}

/** Runs the project's own checks from `:lint-rules` (e.g. `SystemTimeCall`) in this module's lint task. */
internal fun Project.addLintRules() {
    dependencies { add("lintChecks", project(LINT_RULES_PROJECT)) }
}

private const val LINT_RULES_PROJECT = ":lint-rules"
