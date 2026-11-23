package io.github.uveshm003.anchor.buildlogic

import androidx.room.gradle.RoomExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Room via KSP with exported schemas in `<module>/schemas` (committed). The schemas are also test assets, so
 * Robolectric tests can open old versions with `MigrationTestHelper`.
 */
class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("androidx.room")
            pluginManager.apply("com.google.devtools.ksp")
            extensions.configure<RoomExtension> { schemaDirectory("$projectDir/schemas") }
            extensions.configure<LibraryExtension> {
                sourceSets.getByName("test").assets.directories.add("$projectDir/schemas")
            }
            dependencies {
                add("implementation", libs.lib("androidx-room-runtime"))
                add("implementation", libs.lib("androidx-room-ktx"))
                add("ksp", libs.lib("androidx-room-compiler"))
                add("testImplementation", libs.lib("androidx-room-testing"))
            }
        }
    }
}
