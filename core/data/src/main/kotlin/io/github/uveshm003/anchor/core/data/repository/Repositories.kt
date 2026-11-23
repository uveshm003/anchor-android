package io.github.uveshm003.anchor.core.data.repository

import io.github.uveshm003.anchor.core.engine.EngineState
import io.github.uveshm003.anchor.core.model.Anchor
import io.github.uveshm003.anchor.core.model.AnchorId
import io.github.uveshm003.anchor.core.model.Mode
import io.github.uveshm003.anchor.core.model.ModeId
import io.github.uveshm003.anchor.core.model.Session
import kotlinx.coroutines.flow.Flow

/** Features reach data only through these (brief §3). They expose `core:model` types only. */
interface ModeRepository {
    /** Ordered by priority (0 first). */
    val modes: Flow<List<Mode>>

    suspend fun getModes(): List<Mode>

    /** Inserts when the id is 0, updates otherwise; returns the mode's id. */
    suspend fun upsert(mode: Mode): ModeId

    suspend fun delete(id: ModeId)

    /** The list order becomes the priority order, atomically. */
    suspend fun reorder(ordered: List<ModeId>)
}

interface AnchorRepository {
    val anchors: Flow<List<Anchor>>

    suspend fun getAnchors(): List<Anchor>

    suspend fun upsert(anchor: Anchor): AnchorId

    suspend fun delete(id: AnchorId)
}

interface SessionRepository {
    /** The full engine state (active session, unlock history). */
    val engineState: Flow<EngineState>

    val activeSession: Flow<Session?>

    /** Ended sessions, newest first. */
    val history: Flow<List<Session>>

    /** Records that a blocked app was opened during [session]. */
    suspend fun recordBlockAttempt(session: Session, packageName: String)
}

interface UnlockHistoryRepository {
    /** Emergency unlocks left in the rolling 7-day window, from the injected Clock and the quota setting. */
    val remainingEmergencyUnlocks: Flow<Int>
}

data class UserSettings(
    val allowDefaultSmsApp: Boolean,
    val emergencyQuotaPerWeek: Int,
    val onboardingCompleted: Boolean,
    val defaultModeId: ModeId?,
)

interface SettingsRepository {
    val settings: Flow<UserSettings>

    suspend fun setOnboardingCompleted(completed: Boolean)

    suspend fun setDefaultMode(id: ModeId?)

    suspend fun setAllowDefaultSmsApp(allow: Boolean)

    /** Rejected while a session is active (the quota can't be raised to escape one). */
    suspend fun setEmergencyQuota(perWeek: Int): QuotaChange
}

enum class QuotaChange { Changed, RejectedSessionActive, RejectedOutOfRange }
