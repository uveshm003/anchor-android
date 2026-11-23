package io.github.uveshm003.anchor.core.triggers.timer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.uveshm003.anchor.core.engine.Command
import io.github.uveshm003.anchor.core.engine.CommandExecutor
import io.github.uveshm003.anchor.core.engine.ExecutionResult
import javax.inject.Inject

/**
 * Session timers with AlarmManager (RTC, wall-clock UTC, ADR 0003 §5). Exact alarms when
 * `canScheduleExactAlarms()` (SCHEDULE_EXACT_ALARM is user-granted and denied by default for new installs on
 * API 34+), otherwise `setAndAllowWhileIdle`, which may fire some minutes late in Doze. One session runs at a
 * time, so one alarm slot is enough; app-start reconcile ends sessions whose timer passed while the alarm was lost.
 * https://developer.android.com/develop/background-work/services/alarms/schedule (checked 2026-11-23)
 */
class TimerExecutor @Inject constructor(@ApplicationContext private val context: Context) : CommandExecutor {
    override val handles = setOf(Command.ScheduleTimer::class, Command.CancelTimer::class)

    private val alarms = context.getSystemService(AlarmManager::class.java)

    override suspend fun execute(command: Command): ExecutionResult {
        when (command) {
            is Command.ScheduleTimer -> schedule(command)
            is Command.CancelTimer -> alarms.cancel(pendingIntent(command.sessionId.value))
            else -> return ExecutionResult.Failed("Not a timer command: ${command::class.simpleName}")
        }
        return ExecutionResult.Done
    }

    private fun schedule(command: Command.ScheduleTimer) {
        val at = command.at.toEpochMilli()
        val intent = pendingIntent(command.sessionId.value)
        if (alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        }
    }

    private fun pendingIntent(sessionId: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, TimerReceiver::class.java).putExtra(TimerReceiver.EXTRA_SESSION_ID, sessionId),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        const val REQUEST_CODE = 1
    }
}
