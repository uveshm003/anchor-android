package io.github.uveshm003.anchor.feature.home.emergency

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.uveshm003.anchor.core.data.repository.SessionRepository
import io.github.uveshm003.anchor.core.data.repository.SettingsRepository
import io.github.uveshm003.anchor.core.data.repository.UnlockHistoryRepository
import io.github.uveshm003.anchor.core.data.session.SessionRequests
import io.github.uveshm003.anchor.core.engine.Input
import io.github.uveshm003.anchor.core.engine.Outcome
import io.github.uveshm003.anchor.core.engine.RejectReason
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class EmergencyStep { Intro, Hold, Type, Done, Exhausted, RecoveryStarted, NoSession }

data class EmergencyUiState(
    val step: EmergencyStep = EmergencyStep.Intro,
    val left: Int = 0,
    val quota: Int = 0,
    /** Index into the localised sentence list. */
    val sentenceIndex: Int = 0,
    val typed: String = "",
    val mismatch: Boolean = false,
    /** A lost-anchor recovery is already counting down for this session. */
    val recoveryPending: Boolean = false,
)

/**
 * Friction for an emergency unlock: a 10 s hold, then typing a displayed sentence exactly (case-insensitive,
 * no paste). Success ends the session through the engine; the quota is the engine's.
 */
@HiltViewModel
class EmergencyUnlockViewModel @Inject constructor(
    savedState: SavedStateHandle,
    sessions: SessionRepository,
    settings: SettingsRepository,
    unlocks: UnlockHistoryRepository,
    private val requests: SessionRequests,
) : ViewModel() {
    private val flow = MutableStateFlow(
        EmergencyUiState(
            sentenceIndex =
                savedState[SENTENCE_KEY] ?: Random.nextInt(SENTENCE_COUNT).also { savedState[SENTENCE_KEY] = it },
        ),
    )

    val uiState: StateFlow<EmergencyUiState> = combine(
        flow,
        sessions.engineState,
        unlocks.remainingEmergencyUnlocks,
        settings.settings,
    ) {
            state,
            engine,
            left,
            user,
        ->
        val step = when {
            state.step == EmergencyStep.Done -> EmergencyStep.Done
            state.step == EmergencyStep.RecoveryStarted -> EmergencyStep.RecoveryStarted
            engine.active == null -> EmergencyStep.NoSession
            left == 0 && state.step == EmergencyStep.Intro -> EmergencyStep.Exhausted
            else -> state.step
        }
        state.copy(
            step = step,
            left = left,
            quota = user.emergencyQuotaPerWeek,
            recoveryPending =
                engine.recovery != null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), flow.value)

    fun begin() = flow.update { it.copy(step = EmergencyStep.Hold) }

    fun holdCompleted() = flow.update { it.copy(step = EmergencyStep.Type) }

    /**
     * Typing only: a change that adds more than a couple of characters at once (a paste) is rejected.
     * Returns whether the change was accepted, synchronously, so the text field can stay in step with fast typing.
     */
    fun type(text: String): Boolean {
        if (text.length - flow.value.typed.length > MAX_CHARS_PER_CHANGE) return false
        flow.update { it.copy(typed = text, mismatch = false) }
        return true
    }

    /** [expected] is the displayed sentence (from resources). */
    fun submit(expected: String) {
        val state = flow.value
        if (!state.typed.trim().equals(expected.trim(), ignoreCase = true)) {
            flow.update { it.copy(mismatch = true) }
            return
        }
        viewModelScope.launch {
            val outcome = requests.request(Input.EmergencyUnlock)
            val step = when {
                outcome is Outcome.Ended -> EmergencyStep.Done
                outcome is Outcome.Rejected && outcome.reason is RejectReason.QuotaExhausted -> EmergencyStep.Exhausted
                else -> EmergencyStep.NoSession
            }
            flow.update { it.copy(step = step) }
        }
    }

    /** Lost anchor and no unlocks left: end the session after a 24 h delay (cancellable from Home). */
    fun startRecovery() {
        viewModelScope.launch {
            val outcome = requests.request(Input.RecoveryRequested)
            val started = outcome is Outcome.RecoveryScheduled ||
                (outcome is Outcome.Rejected && outcome.reason == RejectReason.RecoveryAlreadyPending)
            if (started) flow.update { it.copy(step = EmergencyStep.RecoveryStarted) }
        }
    }

    companion object {
        /** Must match the size of the `feature_home_emergency_sentences` string array. */
        const val SENTENCE_COUNT = 5
        private const val SENTENCE_KEY = "sentence"
        private const val MAX_CHARS_PER_CHANGE = 2
        private const val STOP_TIMEOUT_MS = 5_000L
    }
}
