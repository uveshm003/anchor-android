package io.github.uveshm003.anchor.core.model

import java.time.Instant
import kotlinx.serialization.Serializable

/** One run of a mode. Only one session is active at a time. Instants are UTC. */
@Serializable
data class Session(
    val id: SessionId,
    val modeId: ModeId,
    val modeName: String,
    @Serializable(with = InstantAsEpochMillis::class) val startedAt: Instant,
    val startedBy: SessionSource,
    val exitRule: ExitRule,
    @Serializable(with = InstantAsEpochMillis::class) val endsAt: Instant? = null,
    @Serializable(with = InstantAsEpochMillis::class) val endedAt: Instant? = null,
    val endedBy: SessionEnd? = null,
    val blockAttempts: Int = 0,
) {
    init {
        require(endsAt == null || endsAt.isAfter(startedAt)) { "endsAt must be after startedAt" }
        require(endedAt == null || !endedAt.isBefore(startedAt)) { "endedAt must not be before startedAt" }
        require((endedAt == null) == (endedBy == null)) { "endedAt and endedBy go together" }
        require(blockAttempts >= 0) { "blockAttempts must be >= 0" }
    }

    val isActive: Boolean get() = endedAt == null
    val emergencyUnlockUsed: Boolean get() = endedBy == SessionEnd.EmergencyUnlock
}

@Serializable
enum class SessionSource { Hold, AnchorTap, Schedule, Place, Driving, BluetoothDevice, Space, Wifi, Usb }

@Serializable
enum class SessionEnd { AnchorTap, Timer, TriggerEnded, EmergencyUnlock, Recovery }

/** An emergency unlock, counted against the rolling weekly quota. */
@Serializable
data class UnlockRecord(@Serializable(with = InstantAsEpochMillis::class) val at: Instant, val sessionId: SessionId)
