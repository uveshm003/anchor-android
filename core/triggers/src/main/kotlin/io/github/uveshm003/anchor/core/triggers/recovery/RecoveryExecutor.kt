package io.github.uveshm003.anchor.core.triggers.recovery

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
import io.github.uveshm003.anchor.core.model.SessionId
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Lost-anchor recovery checks on the monotonic clock (`ELAPSED_REALTIME_WAKEUP`), so changing the wall clock
 * doesn't move them. Alarms don't survive a reboot; the boot reconcile schedules the next check again.
 */
class RecoveryExecutor @Inject constructor(@ApplicationContext private val context: Context) : CommandExecutor {
    override val handles = setOf(Command.ScheduleRecovery::class, Command.CancelRecovery::class)

    private val alarms = context.getSystemService(AlarmManager::class.java)

    override suspend fun execute(command: Command): ExecutionResult {
        when (command) {
            is Command.ScheduleRecovery -> {
                val intent = pendingIntent(command.sessionId.value)
                if (alarms.canScheduleExactAlarms()) {
                    alarms.setExactAndAllowWhileIdle(
                        AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        command.elapsedRealtimeAt,
                        intent,
                    )
                } else {
                    alarms.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, command.elapsedRealtimeAt, intent)
                }
            }

            is Command.CancelRecovery -> alarms.cancel(pendingIntent(command.sessionId.value))

            else -> return ExecutionResult.Failed("Not a recovery command: ${command::class.simpleName}")
        }
        return ExecutionResult.Done
    }

    private fun pendingIntent(sessionId: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, RecoveryReceiver::class.java).putExtra(RecoveryReceiver.EXTRA_SESSION_ID, sessionId),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        const val REQUEST_CODE = 2
    }
}

/** Forwards the recovery alarm to the engine, which decides whether the delay has really passed. */
@AndroidEntryPoint
class RecoveryReceiver : BroadcastReceiver() {
    @Inject lateinit var inputs: SessionInputs

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1)
        if (sessionId < 0) return
        val pending = goAsync()
        scope.launch {
            try {
                inputs.submit(Input.RecoveryElapsed(SessionId(sessionId)))
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_SESSION_ID = "session_id"
    }
}
