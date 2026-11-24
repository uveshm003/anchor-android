package io.github.uveshm003.anchor.feature.modes

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.uveshm003.anchor.core.data.repository.InstalledApp
import io.github.uveshm003.anchor.core.model.Anchor
import io.github.uveshm003.anchor.core.model.AnchorId
import io.github.uveshm003.anchor.core.model.AnchorType
import io.github.uveshm003.anchor.core.model.AppPolicy
import io.github.uveshm003.anchor.core.model.ExitRule
import io.github.uveshm003.anchor.core.model.Mode
import io.github.uveshm003.anchor.core.model.ModeColor
import io.github.uveshm003.anchor.core.model.ModeIcon
import io.github.uveshm003.anchor.core.model.ModeId
import io.github.uveshm003.anchor.core.testing.MainDispatcherRule
import io.github.uveshm003.anchor.core.testing.data.FakeAnchorRepository
import io.github.uveshm003.anchor.core.testing.data.FakeInstalledAppsRepository
import io.github.uveshm003.anchor.core.testing.data.FakeModeRepository
import io.github.uveshm003.anchor.core.testing.data.FakeSettingsRepository
import io.github.uveshm003.anchor.feature.modes.editor.ExitChoice
import io.github.uveshm003.anchor.feature.modes.editor.ModeEditorEffect
import io.github.uveshm003.anchor.feature.modes.editor.ModeEditorViewModel
import io.github.uveshm003.anchor.feature.modes.list.ModesEffect
import io.github.uveshm003.anchor.feature.modes.list.ModesViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ModesViewModelsTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val work = Mode(ModeId(1), "Work", priority = 0)
    private val study = Mode(ModeId(2), "Study", priority = 1)
    private val apps = listOf(
        InstalledApp("com.android.dialer", "Phone"),
        InstalledApp("com.social", "Social"),
        InstalledApp("com.video", "Video"),
        InstalledApp("com.maps", "Maps"),
    )

    @Test
    fun list_loads_reorders_deletes_and_undoes() = runTest {
        val repo = FakeModeRepository(listOf(work, study))
        val vm = ModesViewModel(repo)
        vm.uiState.test {
            var loaded = awaitItem()
            while (loaded.loading) loaded = awaitItem()
            assertThat(loaded.modes.map { it.name }).containsExactly("Work", "Study").inOrder()

            vm.moveDown(work.id)
            assertThat(awaitItem().modes.map { it.name }).containsExactly("Study", "Work").inOrder()
            vm.moveDown(work.id) // already last: no change
            vm.moveUp(work.id)
            assertThat(awaitItem().modes.map { it.name }).containsExactly("Work", "Study").inOrder()

            vm.effects.test {
                vm.delete(study)
                assertThat(awaitItem()).isEqualTo(ModesEffect.Deleted(study))
            }
            assertThat(awaitItem().modes.map { it.name }).containsExactly("Work")
            vm.undoDelete(study)
            assertThat(awaitItem().modes.map { it.name }).containsExactly("Work", "Study").inOrder()
        }
    }

    private fun editor(
        modeId: Long = 0,
        repo: FakeModeRepository = FakeModeRepository(),
        settings: FakeSettingsRepository = FakeSettingsRepository(),
        handle: SavedStateHandle = SavedStateHandle(mapOf("modeId" to modeId)),
        anchorRepo: FakeAnchorRepository = FakeAnchorRepository(),
    ) = ModeEditorViewModel(handle, repo, settings, FakeInstalledAppsRepository(apps), anchorRepo) {
        setOf("com.android.dialer")
    }

    @Test
    fun new_mode_validates_name_and_saves_a_block_list_without_essentials() = runTest {
        val repo = FakeModeRepository(listOf(work))
        val settings = FakeSettingsRepository()
        val vm = editor(repo = repo, settings = settings)
        vm.uiState.test {
            while (awaitItem().loading) Unit
            vm.save()
            assertThat(awaitItem().nameError).isTrue()

            vm.setName("  Deep Work ")
            vm.setIcon(ModeIcon.Study)
            vm.setColor(ModeColor.Indigo)
            vm.toggleApp("com.social")
            vm.toggleApp("com.android.dialer") // essential: ignored
            vm.setExit(ExitChoice.Timer(45))
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(vm.uiState.value.dirty).isTrue()
        vm.effects.test {
            vm.save()
            assertThat(awaitItem()).isEqualTo(ModeEditorEffect.Saved)
        }

        val saved = repo.getModes().single { it.name == "Deep Work" }
        assertThat(saved.appPolicy).isEqualTo(AppPolicy.BlockList(setOf("com.social")))
        assertThat(saved.exitRule).isEqualTo(ExitRule.TimerEnds(45))
        assertThat(saved.icon).isEqualTo(ModeIcon.Study)
        assertThat(saved.priority).isEqualTo(1)
        assertThat(settings.settings.first().defaultModeId).isEqualTo(saved.id)
    }

    @Test
    fun search_and_select_all_shown_skips_essentials_and_allow_list_saves() = runTest {
        val repo = FakeModeRepository()
        val vm = editor(repo = repo)
        vm.uiState.test {
            while (awaitItem().loading) Unit
            vm.showAppPicker(true)
            vm.setQuery("o") // Phone, Social, Video
            assertThat(expectMostRecentItem().shownApps.map { it.label }).containsExactly("Phone", "Social", "Video")
            vm.selectAllShown()
            vm.showAppPicker(false)
            val state = expectMostRecentItem()
            assertThat(state.pickingApps).isFalse()
            assertThat(state.draft.packages).containsExactly("com.social", "com.video")
            vm.setAllowList(true)
            vm.setName("Only")
            vm.setExit(ExitChoice.AnyAnchor)
            cancelAndIgnoreRemainingEvents()
        }
        vm.effects.test {
            vm.save()
            awaitItem()
        }
        val saved = repo.getModes().single()
        assertThat(saved.appPolicy).isEqualTo(AppPolicy.AllowList(setOf("com.social", "com.video")))
        assertThat(saved.exitRule).isEqualTo(ExitRule.RequiresAnchor())
    }

    @Test
    fun editing_loads_the_mode_and_a_saved_draft_survives_process_death() = runTest {
        val repo = FakeModeRepository(listOf(work.copy(appPolicy = AppPolicy.BlockList(setOf("com.video")))))
        val handle = SavedStateHandle(mapOf("modeId" to 1L))
        val first = editor(modeId = 1, repo = repo, handle = handle)
        first.uiState.test {
            var state = awaitItem()
            while (state.loading) state = awaitItem()
            assertThat(state.isNew).isFalse()
            assertThat(state.dirty).isFalse()
            assertThat(state.draft.packages).containsExactly("com.video")
            first.setName("Work 2")
            cancelAndIgnoreRemainingEvents()
        }

        val restored = editor(modeId = 1, repo = repo, handle = handle)
        restored.uiState.test {
            var state = awaitItem()
            while (state.loading || state.draft.name != "Work 2") state = awaitItem()
            assertThat(state.dirty).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun a_specific_anchor_can_be_required_once_one_is_paired() = runTest {
        val desk = Anchor(AnchorId(4), "Desk", AnchorType.Nfc, "a".repeat(64))
        val repo = FakeModeRepository()
        val vm = editor(repo = repo, anchorRepo = FakeAnchorRepository(listOf(desk)))
        vm.uiState.test {
            var state = awaitItem()
            while (state.loading || state.anchors.isEmpty()) state = awaitItem()
            vm.setName("Sleep")
            vm.setExit(ExitChoice.SpecificAnchor(desk.id.value))
            assertThat(expectMostRecentItem().draft.anchorId).isEqualTo(4)
            cancelAndIgnoreRemainingEvents()
        }
        vm.effects.test {
            vm.save()
            awaitItem()
        }
        assertThat(repo.getModes().single().exitRule).isEqualTo(ExitRule.RequiresAnchor(desk.id))
    }
}
