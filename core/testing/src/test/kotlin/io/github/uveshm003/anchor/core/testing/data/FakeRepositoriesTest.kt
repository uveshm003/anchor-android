package io.github.uveshm003.anchor.core.testing.data

import com.google.common.truth.Truth.assertThat
import io.github.uveshm003.anchor.core.data.repository.QuotaChange
import io.github.uveshm003.anchor.core.engine.Input
import io.github.uveshm003.anchor.core.model.Anchor
import io.github.uveshm003.anchor.core.model.AnchorId
import io.github.uveshm003.anchor.core.model.AnchorType
import io.github.uveshm003.anchor.core.model.Mode
import io.github.uveshm003.anchor.core.model.ModeId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

/** Keeps the fakes honest: same behaviour as the real repositories for what features rely on. */
class FakeRepositoriesTest {
    @Test
    fun modes_upsert_reorder_delete() = runTest {
        val repo = FakeModeRepository()
        val a = repo.upsert(Mode(ModeId(0), "A", priority = 0))
        val b = repo.upsert(Mode(ModeId(0), "B", priority = 1))
        repo.upsert(Mode(a, "A2", priority = 0))

        repo.reorder(listOf(b, a))
        assertThat(repo.modes.first().map { it.name }).containsExactly("B", "A2").inOrder()

        repo.delete(b)
        assertThat(repo.getModes().map { it.id }).containsExactly(a)
    }

    @Test
    fun anchors_upsert_delete() = runTest {
        val repo = FakeAnchorRepository()
        val id = repo.upsert(Anchor(AnchorId(0), "Desk", AnchorType.Nfc, "a".repeat(64)))
        assertThat(repo.getAnchors().single().id).isEqualTo(id)
        repo.delete(id)
        assertThat(repo.anchors.first()).isEmpty()
    }

    @Test
    fun settings_reject_quota_changes_during_a_session() = runTest {
        var active = false
        val repo = FakeSettingsRepository(sessionActive = { active })
        assertThat(repo.setEmergencyQuota(2)).isEqualTo(QuotaChange.Changed)
        assertThat(repo.setEmergencyQuota(9)).isEqualTo(QuotaChange.RejectedOutOfRange)
        active = true
        assertThat(repo.setEmergencyQuota(5)).isEqualTo(QuotaChange.RejectedSessionActive)
        repo.setOnboardingCompleted(true)
        repo.setDefaultMode(ModeId(1))
        repo.setAllowDefaultSmsApp(false)
        assertThat(repo.settings.value.emergencyQuotaPerWeek).isEqualTo(2)
        assertThat(repo.settings.value.onboardingCompleted).isTrue()
    }

    @Test
    fun sessions_unlocks_and_inputs_record() = runTest {
        val sessions = FakeSessionRepository()
        assertThat(sessions.activeSession.first()).isNull()
        assertThat(FakeUnlockHistoryRepository(2).remainingEmergencyUnlocks.first()).isEqualTo(2)
        val inputs = FakeSessionInputs()
        inputs.submit(Input.StopRequested)
        assertThat(inputs.submitted).containsExactly(Input.StopRequested)
    }
}
