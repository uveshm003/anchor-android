package io.github.uveshm003.anchor.core.engine

import io.github.uveshm003.anchor.core.model.SessionId
import java.time.Duration

/**
 * Lost-anchor recovery timing (ADR 0003 §5). Counts 24 h of monotonic time (`elapsedRealtime`, which keeps
 * running in deep sleep) accumulated per boot segment, so moving the wall clock can't shorten it. A reboot pauses
 * it (time since the last stamp in the old boot is lost) but never resets it, and a wall-clock limit of 48 h
 * after the request guarantees it completes even if the accounting is lost.
 */
object Recovery {
    val DELAY: Duration = Duration.ofHours(24)
    val WALL_CLOCK_LIMIT: Duration = Duration.ofHours(48)

    fun start(sessionId: SessionId, env: Environment) = PendingRecovery(
        sessionId = sessionId,
        requestedAt = env.now,
        countedMillis = 0,
        stampBoot = env.bootCount,
        stampElapsed = env.elapsedRealtimeMillis,
    )

    /** Adds the monotonic time since the stamp (same boot only) and stamps again at [env]. */
    fun restamp(recovery: PendingRecovery, env: Environment): PendingRecovery {
        val added = if (env.bootCount == recovery.stampBoot) {
            (env.elapsedRealtimeMillis - recovery.stampElapsed).coerceAtLeast(0)
        } else {
            0
        }
        return recovery.copy(
            countedMillis = recovery.countedMillis + added,
            stampBoot = env.bootCount,
            stampElapsed = env.elapsedRealtimeMillis,
        )
    }

    /** For a freshly [restamp]ed recovery. */
    fun isComplete(recovery: PendingRecovery, env: Environment): Boolean =
        recovery.countedMillis >= DELAY.toMillis() || !env.now.isBefore(recovery.requestedAt.plus(WALL_CLOCK_LIMIT))

    /** Monotonic time still to go, for a freshly [restamp]ed recovery. */
    fun remaining(recovery: PendingRecovery): Duration =
        Duration.ofMillis((DELAY.toMillis() - recovery.countedMillis).coerceAtLeast(0))
}
