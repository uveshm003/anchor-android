package io.github.uveshm003.anchor.core.triggers.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.uveshm003.anchor.core.common.dispatchers.ApplicationScope
import io.github.uveshm003.anchor.core.engine.Command
import io.github.uveshm003.anchor.core.engine.CommandExecutor
import io.github.uveshm003.anchor.core.engine.ExecutionResult
import io.github.uveshm003.anchor.core.engine.Input
import io.github.uveshm003.anchor.core.engine.SessionInputs
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * One wake-up for the next schedule opening or closing (the engine works out which). Exact when the user allows
 * exact alarms; otherwise a 10-minute window, so a schedule may start or end up to 10 minutes late (the editor
 * says so). https://developer.android.com/develop/background-work/services/alarms/schedule (checked 2026-11-27)
 */
class ScheduleWakeExecutor @Inject constructor(@ApplicationContext private val context: Context) : CommandExecutor {
    override val handles = setOf(Command.ScheduleWake::class)

    private val alarms = context.getSystemService(AlarmManager::class.java)

    override suspend fun execute(command: Command): ExecutionResult {
        val wake = command as? Command.ScheduleWake
            ?: return ExecutionResult.Failed("Not a schedule command: ${command::class.simpleName}")
        val intent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ScheduleReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val at = wake.at?.toEpochMilli()
        when {
            at == null -> alarms.cancel(intent)
            alarms.canScheduleExactAlarms() -> alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
            else -> alarms.setWindow(AlarmManager.RTC_WAKEUP, at, WINDOW_MS, intent)
        }
        return ExecutionResult.Done
    }

    companion object {
        private const val REQUEST_CODE = 3

        /** How late an inexact schedule alarm may fire. */
        const val WINDOW_MS = 10 * 60 * 1000L
    }
}

/** Forwards the schedule wake-up to the engine; no logic here. */
@AndroidEntryPoint
class ScheduleReceiver : BroadcastReceiver() {
    @Inject lateinit var inputs: SessionInputs

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        scope.launch {
            try {
                inputs.submit(Input.ScheduleTick)
            } finally {
                pending.finish()
            }
        }
    }
}
