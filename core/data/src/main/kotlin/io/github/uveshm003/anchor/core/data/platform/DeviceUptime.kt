package io.github.uveshm003.anchor.core.data.platform

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.uveshm003.anchor.core.engine.Environment
import io.github.uveshm003.anchor.core.engine.PendingRecovery
import io.github.uveshm003.anchor.core.engine.Recovery
import java.time.Clock
import java.time.Duration
import javax.inject.Inject

/** Monotonic time since boot (including deep sleep) and which boot this is: tamper-resistant durations. */
interface DeviceUptime {
    fun elapsedRealtimeMillis(): Long

    fun bootCount(): Int
}

internal class AndroidDeviceUptime @Inject constructor(@ApplicationContext private val context: Context) :
    DeviceUptime {
    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()

    override fun bootCount(): Int = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0)
}

/** Time left on a lost-anchor recovery, right now (for the countdown). */
class RecoveryCountdown @Inject constructor(private val uptime: DeviceUptime, private val clock: Clock) {
    fun remaining(recovery: PendingRecovery): Duration {
        val env = Environment(
            now = clock.instant(),
            elapsedRealtimeMillis = uptime.elapsedRealtimeMillis(),
            bootCount = uptime.bootCount(),
        )
        val stamped = Recovery.restamp(recovery, env)
        val untilLimit = Duration.between(env.now, recovery.requestedAt.plus(Recovery.WALL_CLOCK_LIMIT))
        return minOf(Recovery.remaining(stamped), untilLimit).coerceAtLeast(Duration.ZERO)
    }
}
