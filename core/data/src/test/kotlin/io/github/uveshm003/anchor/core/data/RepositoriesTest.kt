package io.github.uveshm003.anchor.core.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.uveshm003.anchor.core.common.time.TestClock
import io.github.uveshm003.anchor.core.data.repository.OfflineAnchorRepository
import io.github.uveshm003.anchor.core.data.repository.OfflineModeRepository
import io.github.uveshm003.anchor.core.data.repository.OfflineSessionRepository
import io.github.uveshm003.anchor.core.data.repository.OfflineSettingsRepository
import io.github.uveshm003.anchor.core.data.repository.OfflineUnlockHistoryRepository
import io.github.uveshm003.anchor.core.data.repository.QuotaChange
import io.github.uveshm003.anchor.core.data.session.EngineJson
import io.github.uveshm003.anchor.core.database.entity.EngineStateEntity
import io.github.uveshm003.anchor.core.datastore.SettingsDataSource
import io.github.uveshm003.anchor.core.datastore.di.settingsDataStore
import io.github.uveshm003.anchor.core.engine.ActiveSession
import io.github.uveshm003.anchor.core.engine.EngineState
import io.github.uveshm003.anchor.core.model.Anchor
import io.github.uveshm003.anchor.core.model.AnchorId
import io.github.uveshm003.anchor.core.model.AnchorType
import io.github.uveshm003.anchor.core.model.ExitRule
import io.github.uveshm003.anchor.core.model.Mode
import io.github.uveshm003.anchor.core.model.ModeId
import io.github.uveshm003.anchor.core.model.Session
import io.github.uveshm003.anchor.core.model.SessionId
import io.github.uveshm003.anchor.core.model.SessionSource
import io.github.uveshm003.anchor.core.model.UnlockRecord
import java.io.File
import java.time.Duration
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoriesTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val db = inMemoryDb()
    private val clock = TestClock()
    private val modes = OfflineModeRepository(db.modeDao())
    private val anchors = OfflineAnchorRepository(db.anchorDao())
    private val sessions = OfflineSessionRepository(db.engineDao(), db.sessionDao(), clock)

    @After
    fun close() = db.close()

    private fun TestScope.settingsSource() = SettingsDataSource(
        settingsDataStore(backgroundScope + UnconfinedTestDispatcher(testScheduler)) { File(tmp.root, "s.json") },
    )

    private val session =
        Session(SessionId(1), ModeId(1), "Work", TestClock().instant(), SessionSource.Hold, ExitRule.RequiresAnchor())

    private suspend fun activate(unlocks: List<UnlockRecord> = emptyList()) {
        val state = EngineState(active = ActiveSession(session, Mode(ModeId(1), "Work")), unlocks = unlocks)
        db.engineDao().putState(EngineStateEntity(json = EngineJson.encodeState(state)))
    }

    @Test
    fun modes_insert_update_reorder_delete() = runTest {
        val work = modes.upsert(Mode(ModeId(0), "Work", priority = 0))
        val study = modes.upsert(Mode(ModeId(0), "Study", priority = 1))
        assertThat(modes.upsert(Mode(study, "Exams", priority = 1))).isEqualTo(study)

        modes.reorder(listOf(study, work))
        assertThat(modes.modes.first().map { it.name }).containsExactly("Exams", "Work").inOrder()

        modes.delete(work)
        assertThat(modes.getModes().map { it.id }).containsExactly(study)
    }

    @Test
    fun anchors_insert_update_delete() = runTest {
        val id = anchors.upsert(Anchor(AnchorId(0), "Desk", AnchorType.Nfc, "a".repeat(64)))
        anchors.upsert(Anchor(id, "Office desk", AnchorType.Nfc, "a".repeat(64)))
        assertThat(anchors.anchors.first().single().name).isEqualTo("Office desk")

        anchors.delete(id)
        assertThat(anchors.getAnchors()).isEmpty()
    }

    @Test
    fun active_session_comes_from_engine_state_and_records_block_attempts() = runTest {
        sessions.activeSession.test {
            assertThat(awaitItem()).isNull()
            activate()
            assertThat(awaitItem()).isEqualTo(session)
        }
        db.sessionDao().upsert(
            io.github.uveshm003.anchor.core.database.entity.SessionEntity(
                1, 1, "Work", 0, SessionSource.Hold, ExitRule.RequiresAnchor(), null, null, null, 0,
            ),
        )
        sessions.recordBlockAttempt(session, "com.social")
        assertThat(db.sessionDao().observeActive().first()?.blockAttempts).isEqualTo(1)
        assertThat(sessions.history.first()).isEmpty()
    }

    @Test
    fun quota_cannot_change_during_a_session_and_unlocks_count_in_a_rolling_week() = runTest {
        val settings = OfflineSettingsRepository(settingsSource(), sessions)
        val unlocks = OfflineUnlockHistoryRepository(sessions, settings, clock)

        assertThat(settings.setEmergencyQuota(2)).isEqualTo(QuotaChange.Changed)
        assertThat(settings.setEmergencyQuota(99)).isEqualTo(QuotaChange.RejectedOutOfRange)
        activate(unlocks = listOf(UnlockRecord(clock.instant(), SessionId(0))))
        assertThat(settings.setEmergencyQuota(5)).isEqualTo(QuotaChange.RejectedSessionActive)
        assertThat(settings.settings.first().emergencyQuotaPerWeek).isEqualTo(2)
        assertThat(unlocks.remainingEmergencyUnlocks.first()).isEqualTo(1)

        clock.advanceBy(Duration.ofDays(7))
        assertThat(unlocks.remainingEmergencyUnlocks.first()).isEqualTo(2)

        settings.setOnboardingCompleted(true)
        settings.setDefaultMode(ModeId(3))
        settings.setAllowDefaultSmsApp(false)
        val user = settings.settings.first()
        assertThat(user.onboardingCompleted).isTrue()
        assertThat(user.defaultModeId).isEqualTo(ModeId(3))
        assertThat(user.allowDefaultSmsApp).isFalse()
    }
}
