package io.github.uveshm003.anchor.feature.modes

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.uveshm003.anchor.core.data.repository.AppCategory
import io.github.uveshm003.anchor.core.data.repository.InstalledApp
import io.github.uveshm003.anchor.core.model.AppPolicy
import io.github.uveshm003.anchor.core.model.ExitRule
import io.github.uveshm003.anchor.core.testing.MainDispatcherRule
import io.github.uveshm003.anchor.core.testing.data.FakeInstalledAppsRepository
import io.github.uveshm003.anchor.core.testing.data.FakeModeRepository
import io.github.uveshm003.anchor.core.testing.data.FakeSettingsRepository
import io.github.uveshm003.anchor.feature.modes.editor.ModeEditorEffect
import io.github.uveshm003.anchor.feature.modes.editor.ModeEditorViewModel
import io.github.uveshm003.anchor.feature.modes.templates.ModeTemplate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ModeTemplatesTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val apps = listOf(
        InstalledApp("com.social", "Social", AppCategory.Social),
        InstalledApp("com.game", "Game", AppCategory.Game),
        InstalledApp("com.video", "Video", AppCategory.Video),
        InstalledApp("com.maps", "Maps", AppCategory.Maps),
        InstalledApp("com.music", "Music", AppCategory.Audio),
        InstalledApp("com.notes", "Notes"),
        InstalledApp("com.android.dialer", "Phone", AppCategory.Social),
    )
    private val essentials = setOf("com.android.dialer")

    @Test
    fun suggestions_come_from_app_categories_without_essentials() {
        assertThat(ModeTemplate.Study.draft("Study", apps, essentials).packages)
            .containsExactly("com.social", "com.game", "com.video")
        assertThat(
            ModeTemplate.Driving.draft("Driving", apps, essentials).packages,
        ).containsExactly("com.maps", "com.music")
    }

    @Test
    fun every_template_opens_prefilled_and_saves_a_valid_mode() = runTest {
        ModeTemplate.entries.forEach { template ->
            val repo = FakeModeRepository()
            val vm = ModeEditorViewModel(
                SavedStateHandle(mapOf("modeId" to 0L, "template" to template.name, "name" to template.name)),
                repo,
                FakeSettingsRepository(),
                FakeInstalledAppsRepository(apps),
            ) { essentials }
            val state = vm.uiState.first { !it.loading }
            assertThat(state.dirty).isTrue() // nothing saved until the user saves
            assertThat(repo.getModes()).isEmpty()

            vm.effects.test {
                vm.save()
                assertThat(awaitItem()).isEqualTo(ModeEditorEffect.Saved)
            }
            val mode = repo.getModes().single()
            assertThat(mode.name).isEqualTo(template.name)
            assertThat(mode.icon).isEqualTo(template.look.icon)
            assertThat(mode.quietActions).isEqualTo(template.quietActions)
            assertThat(mode.appPolicy is AppPolicy.AllowList).isEqualTo(template.apps.allowList)
            assertThat(mode.exitRule).isEqualTo(
                template.timerMinutes?.let {
                    ExitRule.TimerEnds(it)
                } ?: ExitRule.RequiresAnchor(),
            )
            assertThat(mode.appPolicy.packages).doesNotContain("com.android.dialer")
        }
    }
}
