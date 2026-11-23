package io.github.uveshm003.anchor.core.testing.data

import io.github.uveshm003.anchor.core.data.repository.AnchorRepository
import io.github.uveshm003.anchor.core.data.repository.ModeRepository
import io.github.uveshm003.anchor.core.data.repository.QuotaChange
import io.github.uveshm003.anchor.core.data.repository.SessionRepository
import io.github.uveshm003.anchor.core.data.repository.SettingsRepository
import io.github.uveshm003.anchor.core.data.repository.UnlockHistoryRepository
import io.github.uveshm003.anchor.core.data.repository.UserSettings
import io.github.uveshm003.anchor.core.engine.EngineState
import io.github.uveshm003.anchor.core.engine.Input
import io.github.uveshm003.anchor.core.engine.SessionInputs
import io.github.uveshm003.anchor.core.model.Anchor
import io.github.uveshm003.anchor.core.model.AnchorId
import io.github.uveshm003.anchor.core.model.Mode
import io.github.uveshm003.anchor.core.model.ModeId
import io.github.uveshm003.anchor.core.model.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory fakes with the same contracts as the Room/DataStore repositories. */
class FakeModeRepository(initial: List<Mode> = emptyList()) : ModeRepository {
    private val state = MutableStateFlow(initial.sortedBy { it.priority })
    private var nextId = (initial.maxOfOrNull { it.id.value } ?: 0) + 1
    override val modes: StateFlow<List<Mode>> = state

    override suspend fun getModes(): List<Mode> = state.value

    override suspend fun upsert(mode: Mode): ModeId {
        val saved = if (mode.id.value == 0L) mode.copy(id = ModeId(nextId++)) else mode
        state.update { list ->
            (list.filterNot { it.id == saved.id } + saved).sortedWith(compareBy({ it.priority }, { it.id.value }))
        }
        return saved.id
    }

    override suspend fun delete(id: ModeId) = state.update { list -> list.filterNot { it.id == id } }

    override suspend fun reorder(ordered: List<ModeId>) = state.update { list ->
        list.map { mode -> mode.copy(priority = ordered.indexOf(mode.id).takeIf { it >= 0 } ?: mode.priority) }
            .sortedWith(compareBy({ it.priority }, { it.id.value }))
    }
}

class FakeAnchorRepository(initial: List<Anchor> = emptyList()) : AnchorRepository {
    private val state = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id.value } ?: 0) + 1
    override val anchors: StateFlow<List<Anchor>> = state

    override suspend fun getAnchors(): List<Anchor> = state.value

    override suspend fun upsert(anchor: Anchor): AnchorId {
        val saved = if (anchor.id.value == 0L) anchor.copy(id = AnchorId(nextId++)) else anchor
        state.update { list -> list.filterNot { it.id == saved.id } + saved }
        return saved.id
    }

    override suspend fun delete(id: AnchorId) = state.update { list -> list.filterNot { it.id == id } }
}

class FakeSessionRepository(initial: EngineState = EngineState()) : SessionRepository {
    val state = MutableStateFlow(initial)
    val historyState = MutableStateFlow<List<Session>>(emptyList())
    val blockAttempts = mutableListOf<Pair<Session, String>>()

    override val engineState = state
    override val activeSession = state.map { it.active?.session }
    override val history = historyState

    override suspend fun recordBlockAttempt(session: Session, packageName: String) {
        blockAttempts += session to packageName
    }
}

class FakeUnlockHistoryRepository(remaining: Int = 3) : UnlockHistoryRepository {
    val remaining = MutableStateFlow(remaining)
    override val remainingEmergencyUnlocks = this.remaining
}

class FakeSettingsRepository(
    initial: UserSettings = UserSettings(
        allowDefaultSmsApp = true,
        emergencyQuotaPerWeek = 3,
        onboardingCompleted = false,
        defaultModeId = null,
    ),
    private val sessionActive: () -> Boolean = { false },
) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val settings: StateFlow<UserSettings> = state

    override suspend fun setOnboardingCompleted(completed: Boolean) = state.update {
        it.copy(onboardingCompleted = completed)
    }

    override suspend fun setDefaultMode(id: ModeId?) = state.update { it.copy(defaultModeId = id) }

    override suspend fun setAllowDefaultSmsApp(allow: Boolean) = state.update { it.copy(allowDefaultSmsApp = allow) }

    override suspend fun setEmergencyQuota(perWeek: Int): QuotaChange = when {
        perWeek !in 0..MAX_QUOTA -> QuotaChange.RejectedOutOfRange

        sessionActive() -> QuotaChange.RejectedSessionActive

        else -> {
            state.update { it.copy(emergencyQuotaPerWeek = perWeek) }
            QuotaChange.Changed
        }
    }

    private companion object {
        const val MAX_QUOTA = 7
    }
}

/** Records submitted inputs. */
class FakeSessionInputs : SessionInputs {
    val submitted = mutableListOf<Input>()

    override suspend fun submit(input: Input) {
        submitted += input
    }
}

class FakeInstalledAppsRepository(
    initial: List<io.github.uveshm003.anchor.core.data.repository.InstalledApp> = emptyList(),
) : io.github.uveshm003.anchor.core.data.repository.InstalledAppsRepository {
    override val apps = MutableStateFlow(initial)
}
