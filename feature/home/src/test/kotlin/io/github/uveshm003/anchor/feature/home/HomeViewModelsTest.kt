package io.github.uveshm003.anchor.feature.home

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.uveshm003.anchor.core.common.time.TestClock
import io.github.uveshm003.anchor.core.data.platform.DeviceUptime
import io.github.uveshm003.anchor.core.data.platform.RecoveryCountdown
import io.github.uveshm003.anchor.core.data.repository.UserSettings
import io.github.uveshm003.anchor.core.engine.ActiveSession
import io.github.uveshm003.anchor.core.engine.EngineState
import io.github.uveshm003.anchor.core.engine.Input
import io.github.uveshm003.anchor.core.engine.Outcome
import io.github.uveshm003.anchor.core.engine.PendingRecovery
import io.github.uveshm003.anchor.core.engine.Recovery
import io.github.uveshm003.anchor.core.engine.RejectReason
import io.github.uveshm003.anchor.core.model.ExitRule
import io.github.uveshm003.anchor.core.model.Mode
import io.github.uveshm003.anchor.core.model.ModeId
import io.github.uveshm003.anchor.core.model.Session
import io.github.uveshm003.anchor.core.model.SessionEnd
import io.github.uveshm003.anchor.core.model.SessionId
import io.github.uveshm003.anchor.core.model.SessionSource
import io.github.uveshm003.anchor.core.testing.MainDispatcherRule
import io.github.uveshm003.anchor.core.testing.data.FakeAnchorRepository
import io.github.uveshm003.anchor.core.testing.data.FakeBlockerStatus
import io.github.uveshm003.anchor.core.testing.data.FakeModeRepository
import io.github.uveshm003.anchor.core.testing.data.FakeSessionRepository
import io.github.uveshm003.anchor.core.testing.data.FakeSessionRequests
import io.github.uveshm003.anchor.core.testing.data.FakeSettingsRepository
import io.github.uveshm003.anchor.core.testing.data.FakeUnlockHistoryRepository
import io.github.uveshm003.anchor.feature.home.emergency.EmergencyStep
import io.github.uveshm003.anchor.feature.home.emergency.EmergencyUnlockViewModel
import java.time.Duration
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeViewModelsTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val clock = TestClock()
    private val work = Mode(ModeId(1), "Work", priority = 0)
    private val study = Mode(ModeId(2), "Study", priority = 1)
    private val session =
        Session(SessionId(1), work.id, "Work", clock.instant(), SessionSource.Hold, ExitRule.RequiresAnchor())
    private val activeState = EngineState(active = ActiveSession(session, work))
    private val settings = FakeSettingsRepository(UserSettings(true, 3, true, defaultModeId = study.id))
    private val requests = FakeSessionRequests()
    private val blocker = FakeBlockerStatus(enabled = true)
    private val sessions = FakeSessionRepository()
    private var elapsed = 0L
    private val uptime = object : DeviceUptime {
        override fun elapsedRealtimeMillis() = elapsed

        override fun bootCount() = 1
    }

    private fun home() = HomeViewModel(
        FakeModeRepository(listOf(work, study)),
        settings,
        blocker,
        FakeAnchorRepository(),
        SessionSources(sessions, FakeUnlockHistoryRepository(2), requests, RecoveryCountdown(uptime, clock)),
        clock,
    )

    @Test
    fun lists_modes_default_first_and_starts_the_selected_mode() = runTest {
        val vm = home()
        vm.uiState.test {
            var state = awaitItem()
            while (state.loading) state = awaitItem()
            assertThat(state.modes.map { it.name }).containsExactly("Study", "Work").inOrder()
            assertThat(state.selectedModeId).isEqualTo(study.id)
            assertThat(state.emergencyUnlocksLeft).isEqualTo(2)

            vm.select(work.id)
            assertThat(awaitItem().selectedModeId).isEqualTo(work.id)
            vm.start()
            assertThat(requests.requested).containsExactly(Input.HoldToStart(work.id))
        }
    }

    @Test
    fun starting_with_blocking_off_explains_instead_of_starting() = runTest {
        blocker.enabled.value = false
        val vm = home()
        vm.uiState.test {
            while (awaitItem().loading) Unit
            vm.effects.test {
                vm.start()
                assertThat(awaitItem()).isEqualTo(HomeEffect.BlockerOff)
            }
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(requests.requested).isEmpty()
    }

    @Test
    fun rejections_are_surfaced_and_the_active_session_shows_with_a_clock_based_now() = runTest {
        requests.outcome = { Outcome.Rejected(RejectReason.AlreadyActive("Work")) }
        sessions.state.value = activeState
        val vm = home()
        vm.uiState.test {
            var state = awaitItem()
            while (state.loading) state = awaitItem()
            assertThat(state.active).isEqualTo(session)
            vm.effects.test {
                vm.start()
                assertThat(awaitItem()).isEqualTo(HomeEffect.Rejected(RejectReason.AlreadyActive("Work")))
            }
            cancelAndIgnoreRemainingEvents()
        }
        vm.now.test {
            // Elapsed time comes from the session start and the injected Clock, not a counter.
            assertThat(awaitItem()).isEqualTo(clock.instant())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun emergency(left: Int = 2, handle: SavedStateHandle = SavedStateHandle()) =
        EmergencyUnlockViewModel(handle, sessions, settings, FakeUnlockHistoryRepository(left), requests)

    @Test
    fun emergency_unlock_needs_hold_then_the_exact_sentence_without_paste() = runTest {
        sessions.state.value = activeState
        requests.outcome = { Outcome.Ended(session) }
        val vm = emergency()
        vm.uiState.test {
            var state = awaitItem()
            while (state.quota == 0) state = awaitItem()
            assertThat(state.step).isEqualTo(EmergencyStep.Intro)
            assertThat(state.left).isEqualTo(2)

            vm.begin()
            assertThat(awaitItem().step).isEqualTo(EmergencyStep.Hold)
            vm.holdCompleted()
            assertThat(awaitItem().step).isEqualTo(EmergencyStep.Type)

            assertThat(vm.type("I am choosing to end my focus session early")).isFalse() // a paste
            vm.type("i")
            vm.type("i a")
            assertThat(expectMostRecentItem().typed).isEqualTo("i a")
            vm.submit("I am choosing")
            assertThat(awaitItem().mismatch).isTrue()
            assertThat(requests.requested).isEmpty()

            "i am choosing".drop(3).forEach { char -> vm.type(vm.uiState.value.typed + char) }
            vm.submit("I am Choosing ")
            assertThat(expectMostRecentItem().step).isEqualTo(EmergencyStep.Done)
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(requests.requested).containsExactly(Input.EmergencyUnlock)
    }

    @Test
    fun emergency_unlock_explains_an_exhausted_quota_and_a_missing_session() = runTest {
        sessions.state.value = activeState
        emergency(left = 0).uiState.test {
            var state = awaitItem()
            while (state.quota == 0) state = awaitItem()
            assertThat(state.step).isEqualTo(EmergencyStep.Exhausted)
            cancelAndIgnoreRemainingEvents()
        }
        sessions.state.value = EngineState()
        val handle = SavedStateHandle()
        val vm = emergency(handle = handle)
        vm.uiState.test {
            var state = awaitItem()
            while (state.quota == 0) state = awaitItem()
            assertThat(state.step).isEqualTo(EmergencyStep.NoSession)
            cancelAndIgnoreRemainingEvents()
        }
        // The sentence stays the same after process death.
        assertThat(emergency(handle = handle).uiState.value.sentenceIndex).isEqualTo(vm.uiState.value.sentenceIndex)
    }

    @Test
    fun a_quota_rejection_on_submit_shows_the_exhausted_step() = runTest {
        sessions.state.value = activeState
        requests.outcome = { Outcome.Rejected(RejectReason.QuotaExhausted(clock.instant())) }
        val vm = emergency()
        vm.uiState.test {
            while (awaitItem().quota == 0) Unit
            vm.begin()
            vm.holdCompleted()
            vm.submit("")
            assertThat(expectMostRecentItem().step).isEqualTo(EmergencyStep.Exhausted)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun recovery_shows_a_countdown_can_be_cancelled_and_offers_repair_afterwards() = runTest {
        val recovery =
            PendingRecovery(SessionId(1), clock.instant(), countedMillis = 0, stampBoot = 1, stampElapsed = 0)
        sessions.state.value = activeState.copy(recovery = recovery)
        elapsed = Duration.ofHours(2).toMillis()
        val vm = home()
        vm.uiState.test {
            var state = awaitItem()
            while (state.loading) state = awaitItem()
            assertThat(state.recoveryRemaining).isEqualTo(Duration.ofHours(22))
            vm.cancelRecovery()
            assertThat(requests.requested).contains(Input.RecoveryCancelled)

            sessions.state.value = EngineState()
            sessions.historyState.value = listOf(session.copy(endedAt = clock.instant(), endedBy = SessionEnd.Recovery))
            assertThat(expectMostRecentItem().offerRepair).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun recovery_can_be_started_when_the_quota_is_used_up() = runTest {
        sessions.state.value = activeState
        requests.outcome = { Outcome.RecoveryScheduled(Recovery.DELAY) }
        val vm = emergency(left = 0)
        vm.uiState.test {
            var state = awaitItem()
            while (state.quota == 0) state = awaitItem()
            assertThat(state.step).isEqualTo(EmergencyStep.Exhausted)
            assertThat(state.recoveryPending).isFalse()
            vm.startRecovery()
            assertThat(expectMostRecentItem().step).isEqualTo(EmergencyStep.RecoveryStarted)
            cancelAndIgnoreRemainingEvents()
        }
        assertThat(requests.requested).containsExactly(Input.RecoveryRequested)
    }
}
