package io.github.uveshm003.anchor.core.data.repository

import io.github.uveshm003.anchor.core.data.session.EngineJson
import io.github.uveshm003.anchor.core.database.dao.AnchorDao
import io.github.uveshm003.anchor.core.database.dao.EngineDao
import io.github.uveshm003.anchor.core.database.dao.ModeDao
import io.github.uveshm003.anchor.core.database.dao.SessionDao
import io.github.uveshm003.anchor.core.database.entity.BlockAttemptEntity
import io.github.uveshm003.anchor.core.datastore.AnchorSettings
import io.github.uveshm003.anchor.core.datastore.SettingsDataSource
import io.github.uveshm003.anchor.core.engine.EmergencyQuota
import io.github.uveshm003.anchor.core.engine.EngineState
import io.github.uveshm003.anchor.core.model.Anchor
import io.github.uveshm003.anchor.core.model.AnchorId
import io.github.uveshm003.anchor.core.model.Mode
import io.github.uveshm003.anchor.core.model.ModeId
import io.github.uveshm003.anchor.core.model.Session
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal class OfflineModeRepository @Inject constructor(private val dao: ModeDao) : ModeRepository {
    override val modes: Flow<List<Mode>> = dao.observeAll().map { list -> list.map { it.asModel() } }

    override suspend fun getModes(): List<Mode> = dao.getAll().map { it.asModel() }

    override suspend fun upsert(mode: Mode): ModeId {
        val rowId = dao.upsert(mode.asEntity())
        // Upsert returns -1 when it updated an existing row.
        return if (mode.id.value != 0L) mode.id else ModeId(rowId)
    }

    override suspend fun delete(id: ModeId) = dao.delete(id.value)

    override suspend fun reorder(ordered: List<ModeId>) = dao.reorder(ordered.map { it.value })
}

internal class OfflineAnchorRepository @Inject constructor(private val dao: AnchorDao) : AnchorRepository {
    override val anchors: Flow<List<Anchor>> = dao.observeAll().map { list -> list.map { it.asModel() } }

    override suspend fun getAnchors(): List<Anchor> = dao.getAll().map { it.asModel() }

    override suspend fun upsert(anchor: Anchor): AnchorId {
        val rowId = dao.upsert(anchor.asEntity())
        return if (anchor.id.value != 0L) anchor.id else AnchorId(rowId)
    }

    override suspend fun delete(id: AnchorId) = dao.delete(id.value)
}

internal class OfflineSessionRepository @Inject constructor(
    engineDao: EngineDao,
    private val sessionDao: SessionDao,
    private val clock: Clock,
) : SessionRepository {
    override val engineState: Flow<EngineState> = engineDao.observeState().map { row ->
        row?.let { EngineJson.decodeState(it.json) } ?: EngineState()
    }

    override val activeSession: Flow<Session?> = engineState.map { it.active?.session }

    override val history: Flow<List<Session>> = sessionDao.observeHistory().map { list -> list.map { it.asModel() } }

    override suspend fun recordBlockAttempt(session: Session, packageName: String) {
        sessionDao.insertBlockAttempt(BlockAttemptEntity(0, session.id.value, packageName, clock.millis()))
        sessionDao.incrementBlockAttempts(session.id.value)
    }
}

internal class OfflineUnlockHistoryRepository @Inject constructor(
    sessions: SessionRepository,
    settings: SettingsRepository,
    private val clock: Clock,
) : UnlockHistoryRepository {
    override val remainingEmergencyUnlocks: Flow<Int> =
        combine(sessions.engineState, settings.settings) { state, user ->
            EmergencyQuota.remaining(state.unlocks, clock.instant(), user.emergencyQuotaPerWeek)
        }
}

internal class OfflineSettingsRepository @Inject constructor(
    private val source: SettingsDataSource,
    private val sessions: SessionRepository,
) : SettingsRepository {
    override val settings: Flow<UserSettings> = source.settings.map { it.asUserSettings() }

    override suspend fun setOnboardingCompleted(completed: Boolean) =
        source.update { it.copy(onboardingCompleted = completed) }

    override suspend fun setDefaultMode(id: ModeId?) = source.update { it.copy(defaultModeId = id?.value) }

    override suspend fun setAllowDefaultSmsApp(allow: Boolean) = source.update { it.copy(allowDefaultSmsApp = allow) }

    override suspend fun setEmergencyQuota(perWeek: Int): QuotaChange = when {
        perWeek !in AnchorSettings.QUOTA_RANGE -> QuotaChange.RejectedOutOfRange

        sessions.activeSession.first() != null -> QuotaChange.RejectedSessionActive

        else -> {
            source.update { it.copy(emergencyQuotaPerWeek = perWeek) }
            QuotaChange.Changed
        }
    }

    private fun AnchorSettings.asUserSettings() = UserSettings(
        allowDefaultSmsApp = allowDefaultSmsApp,
        emergencyQuotaPerWeek = emergencyQuotaPerWeek,
        onboardingCompleted = onboardingCompleted,
        defaultModeId = defaultModeId?.let(::ModeId),
    )
}
