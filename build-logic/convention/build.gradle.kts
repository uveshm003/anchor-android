import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "io.github.uveshm003.anchor.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.kover.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.roborazzi.gradlePlugin)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = libs.plugins.anchor.android.application.asProvider().get().pluginId
            implementationClass = "io.github.uveshm003.anchor.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidApplicationCompose") {
            id = libs.plugins.anchor.android.application.compose.get().pluginId
            implementationClass = "io.github.uveshm003.anchor.buildlogic.AndroidApplicationComposeConventionPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.anchor.android.library.asProvider().get().pluginId
            implementationClass = "io.github.uveshm003.anchor.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = libs.plugins.anchor.android.library.compose.get().pluginId
            implementationClass = "io.github.uveshm003.anchor.buildlogic.AndroidLibraryComposeConventionPlugin"
        }
        register("androidFeature") {
            id = libs.plugins.anchor.android.feature.get().pluginId
            implementationClass = "io.github.uveshm003.anchor.buildlogic.AndroidFeatureConventionPlugin"
        }
        register("androidTest") {
            id = libs.plugins.anchor.android.test.get().pluginId
            implementationClass = "io.github.uveshm003.anchor.buildlogic.AndroidTestConventionPlugin"
        }
        register("hilt") {
            id = libs.plugins.anchor.hilt.get().pluginId
            implementationClass = "io.github.uveshm003.anchor.buildlogic.HiltConventionPlugin"
        }
        register("jvmLibrary") {
            id = libs.plugins.anchor.jvm.library.get().pluginId
            implementationClass = "io.github.uveshm003.anchor.buildlogic.JvmLibraryConventionPlugin"
        }
        register("lintRules") {
            id = libs.plugins.anchor.lint.rules.get().pluginId
            implementationClass = "io.github.uveshm003.anchor.buildlogic.LintRulesConventionPlugin"
        }
    }
}
