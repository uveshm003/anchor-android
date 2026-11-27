package io.github.uveshm003.anchor.feature.modes.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.uveshm003.anchor.core.data.platform.DeviceSetup
import io.github.uveshm003.anchor.core.data.repository.ModeRepository
import io.github.uveshm003.anchor.core.data.repository.SessionRepository
import io.github.uveshm003.anchor.core.engine.Environment
import io.github.uveshm003.anchor.core.engine.Explain
import io.github.uveshm003.anchor.core.engine.ModeStatus
import io.github.uveshm003.anchor.core.engine.TriggerPermission
import io.github.uveshm003.anchor.core.model.Mode
import io.github.uveshm003.anchor.core.model.ModeId
import java.time.Clock
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ModesUiState(
    val loading: Boolean = true,
    val modes: List<Mode> = emptyList(),
    /** Why each mode is or isn't running (from the engine). */
    val statuses: Map<ModeId, ModeStatus> = emptyMap(),
    /** Permissions a mode's triggers rely on that aren't granted. */
    val missing: Map<ModeId, Set<TriggerPermission>> = emptyMap(),
    val zone: ZoneId = ZoneOffset.UTC,
)

sealed interface ModesEffect {
    /** Offer undo for a deleted mode. */
    data class Deleted(val mode: Mode) : ModesEffect
}

@HiltViewModel
class ModesViewModel @Inject constructor(
    private val repository: ModeRepository,
    sessions: SessionRepository,
    private val clock: Clock,
    private val device: DeviceSetup,
) : ViewModel() {
    val uiState: StateFlow<ModesUiState> = combine(repository.modes, sessions.engineState) { modes, engine ->
        val env = Environment(now = clock.instant(), modes = modes, zone = clock.zone)
        ModesUiState(
            loading = false,
            modes = modes,
            statuses = modes.associate { it.id to Explain.status(it, engine, env) },
            missing = modes.associate { mode -> mode.id to Explain.needs(mode).filterNot(device::granted).toSet() }
                .filterValues { it.isNotEmpty() },
            zone = clock.zone,
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ModesUiState())

    private val _effects = Channel<ModesEffect>(Channel.BUFFERED)
    val effects: Flow<ModesEffect> = _effects.receiveAsFlow()

    fun moveUp(id: ModeId) = move(id, -1)

    fun moveDown(id: ModeId) = move(id, +1)

    fun delete(mode: Mode) {
        viewModelScope.launch {
            repository.delete(mode.id)
            _effects.send(ModesEffect.Deleted(mode))
        }
    }

    /** Restores a deleted mode with its id and priority. */
    fun undoDelete(mode: Mode) {
        viewModelScope.launch { repository.upsert(mode) }
    }

    private fun move(id: ModeId, by: Int) {
        val ids = uiState.value.modes.map { it.id }.toMutableList()
        val from = ids.indexOf(id)
        val to = from + by
        if (from < 0 || to !in ids.indices) return
        ids.add(to, ids.removeAt(from))
        viewModelScope.launch { repository.reorder(ids) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
