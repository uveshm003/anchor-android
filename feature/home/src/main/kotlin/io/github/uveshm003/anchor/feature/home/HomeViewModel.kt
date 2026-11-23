package io.github.uveshm003.anchor.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.uveshm003.anchor.core.data.platform.BlockerStatus
import io.github.uveshm003.anchor.core.data.repository.ModeRepository
import io.github.uveshm003.anchor.core.data.repository.SessionRepository
import io.github.uveshm003.anchor.core.data.repository.SettingsRepository
import io.github.uveshm003.anchor.core.data.repository.UnlockHistoryRepository
import io.github.uveshm003.anchor.core.data.session.SessionRequests
import io.github.uveshm003.anchor.core.engine.Input
import io.github.uveshm003.anchor.core.engine.Outcome
import io.github.uveshm003.anchor.core.engine.RejectReason
import io.github.uveshm003.anchor.core.model.Mode
import io.github.uveshm003.anchor.core.model.ModeId
import io.github.uveshm003.anchor.core.model.Session
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    /** Default mode first, then by priority. */
    val modes: List<Mode> = emptyList(),
    val selectedModeId: ModeId? = null,
    val active: Session? = null,
    val blockerEnabled: Boolean = true,
    val emergencyUnlocksLeft: Int = 0,
    val emergencyQuota: Int = 0,
)

sealed interface HomeEffect {
    data class Rejected(val reason: RejectReason) : HomeEffect

    /** Starting needs the blocker service; show the disclosure and the way to turn it on. */
    data object BlockerOff : HomeEffect
}

/** The running session, the emergency quota and the way to start one. */
class SessionSources @Inject constructor(
    val sessions: SessionRepository,
    val unlocks: UnlockHistoryRepository,
    val requests: SessionRequests,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    modes: ModeRepository,
    settings: SettingsRepository,
    blocker: BlockerStatus,
    private val session: SessionSources,
    private val clock: Clock,
) : ViewModel() {
    private val selected = MutableStateFlow<ModeId?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        combine(modes.modes, settings.settings, selected, ::Triple),
        session.sessions.activeSession,
        blocker.enabled,
        session.unlocks.remainingEmergencyUnlocks,
    ) { (all, user, pick), active, enabled, left ->
        val ordered = all.sortedByDescending { it.id == user.defaultModeId }
        HomeUiState(
            loading = false,
            modes = ordered,
            selectedModeId = pick?.takeIf { id -> ordered.any { it.id == id } } ?: ordered.firstOrNull()?.id,
            active = active,
            blockerEnabled = enabled,
            emergencyUnlocksLeft = left,
            emergencyQuota = user.emergencyQuotaPerWeek,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    /** Wall-clock now for the elapsed time, sampled from the injected Clock (not a counter). */
    val now: StateFlow<Instant> = flow {
        while (true) {
            emit(clock.instant())
            delay(TICK_MS)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), clock.instant())

    val zone: ZoneId get() = clock.zone

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: Flow<HomeEffect> = _effects.receiveAsFlow()

    fun select(id: ModeId) {
        selected.value = id
    }

    /** Called when the hold completes. */
    fun start() {
        val state = uiState.value
        val modeId = state.selectedModeId ?: return
        viewModelScope.launch {
            if (!state.blockerEnabled) {
                _effects.send(HomeEffect.BlockerOff)
                return@launch
            }
            val outcome = session.requests.request(Input.HoldToStart(modeId))
            if (outcome is Outcome.Rejected) _effects.send(HomeEffect.Rejected(outcome.reason))
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val TICK_MS = 15_000L
    }
}
