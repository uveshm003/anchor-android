package io.github.uveshm003.anchor.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("anchor.android.library")
            configureAndroidCompose(extensions.getByType<LibraryExtension>())
            addToModuleCheck("verifyRoborazziDebug")
        }
    }
}
