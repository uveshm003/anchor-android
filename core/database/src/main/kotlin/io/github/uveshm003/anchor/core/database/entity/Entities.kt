package io.github.uveshm003.anchor.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.uveshm003.anchor.core.model.AnchorType
import io.github.uveshm003.anchor.core.model.AppPolicy
import io.github.uveshm003.anchor.core.model.ExitRule
import io.github.uveshm003.anchor.core.model.ModeColor
import io.github.uveshm003.anchor.core.model.ModeIcon
import io.github.uveshm003.anchor.core.model.QuietActions
import io.github.uveshm003.anchor.core.model.SessionEnd
import io.github.uveshm003.anchor.core.model.SessionSource
import io.github.uveshm003.anchor.core.model.StartTrigger

/** Instants are epoch millis UTC. Structured rules are JSON columns (see [Converters]). */
@Entity(tableName = "modes", indices = [Index("priority")])
data class ModeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: ModeIcon,
    val color: ModeColor,
    val priority: Int,
    val appPolicy: AppPolicy,
    val quietActions: QuietActions,
    val startTriggers: Set<StartTrigger>,
    val exitRule: ExitRule,
    val strict: Boolean,
)

@Entity(tableName = "anchors", indices = [Index("hash", unique = true)])
data class AnchorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: AnchorType,
    val hash: String,
    val modeId: Long?,
)

/** Session history. Ids come from the engine, so they are not auto-generated. */
@Entity(tableName = "sessions", indices = [Index("startedAt"), Index("endedAt")])
data class SessionEntity(
    @PrimaryKey val id: Long,
    val modeId: Long,
    val modeName: String,
    val startedAt: Long,
    val startedBy: SessionSource,
    val exitRule: ExitRule,
    val endsAt: Long?,
    val endedAt: Long?,
    val endedBy: SessionEnd?,
    val blockAttempts: Int,
)

/** A blocked app was opened during a session (Weekly Activity counts these per app). */
@Entity(tableName = "block_attempts", indices = [Index("sessionId")])
data class BlockAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val packageName: String,
    val at: Long,
)

/** Emergency unlock log for insights (the quota itself is engine state). */
@Entity(tableName = "emergency_unlocks", indices = [Index("at")])
data class EmergencyUnlockEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val sessionId: Long, val at: Long)

/** The engine state, persisted as one JSON snapshot in a single row (ADR 0003). */
@Entity(tableName = "engine_state")
data class EngineStateEntity(@PrimaryKey val id: Int = SINGLE_ROW, val json: String) {
    companion object {
        const val SINGLE_ROW = 0
    }
}

/** Commands of a transition, persisted with the state and executed in [sequence] order (ADR 0003 §3). */
@Entity(tableName = "outbox", indices = [Index("status"), Index("sequence")])
data class OutboxEntity(
    @PrimaryKey val commandId: String,
    val sequence: Long,
    val json: String,
    val status: OutboxStatus,
)

enum class OutboxStatus { Pending, Done, Failed }
