package io.github.uveshm003.anchor.core.datastore

import com.google.common.truth.Truth.assertThat
import io.github.uveshm003.anchor.core.datastore.di.settingsDataStore
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class SettingsDataSourceTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun TestScope.source(file: File) =
        SettingsDataSource(settingsDataStore(backgroundScope + UnconfinedTestDispatcher(testScheduler)) { file })

    @Test
    fun defaults_then_updates() = runTest {
        val source = source(File(tmp.root, "settings.json"))
        assertThat(source.settings.first()).isEqualTo(AnchorSettings())
        assertThat(AnchorSettings().emergencyQuotaPerWeek).isEqualTo(3)
        assertThat(AnchorSettings().allowDefaultSmsApp).isTrue()

        source.update { it.copy(onboardingCompleted = true, defaultModeId = 4, emergencyQuotaPerWeek = 2) }

        assertThat(source.settings.first())
            .isEqualTo(AnchorSettings(onboardingCompleted = true, defaultModeId = 4, emergencyQuotaPerWeek = 2))
    }

    @Test
    fun install_salt_is_created_once_and_kept() = runTest {
        val source = source(File(tmp.root, "settings.json"))

        val salt = source.installSalt()

        assertThat(salt).matches("[0-9a-f]{64}")
        assertThat(source.installSalt()).isEqualTo(salt)
        assertThat(source.settings.first().installSalt).isEqualTo(salt)
    }

    @Test
    fun corrupt_file_falls_back_to_defaults() = runTest {
        val file = File(tmp.root, "settings.json").apply { writeText("{not json") }

        assertThat(source(file).settings.first()).isEqualTo(AnchorSettings())
    }

    @Test
    fun wrong_types_fall_back_to_defaults() = runTest {
        val file = File(tmp.root, "settings.json").apply { writeText("{\"emergencyQuotaPerWeek\":\"three\"}") }

        assertThat(source(file).settings.first()).isEqualTo(AnchorSettings())
    }
}
