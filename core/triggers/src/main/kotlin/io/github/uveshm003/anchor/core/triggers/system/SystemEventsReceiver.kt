package io.github.uveshm003.anchor.core.triggers.system

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import io.github.uveshm003.anchor.core.common.dispatchers.ApplicationScope
import io.github.uveshm003.anchor.core.engine.Input
import io.github.uveshm003.anchor.core.engine.ReconcileCause
import io.github.uveshm003.anchor.core.engine.SessionInputs
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Reboot, app update and clock/time-zone changes: reconcile, which re-applies an active session and schedules its
 * timer and recovery alarms again (alarms are cleared on reboot). No logic here.
 */
@AndroidEntryPoint
class SystemEventsReceiver : BroadcastReceiver() {
    @Inject lateinit var inputs: SessionInputs

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val cause = when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> ReconcileCause.Boot

            Intent.ACTION_MY_PACKAGE_REPLACED -> ReconcileCause.PackageReplaced

            Intent.ACTION_TIME_CHANGED -> ReconcileCause.TimeChanged

            Intent.ACTION_TIMEZONE_CHANGED -> ReconcileCause.TimeZoneChanged

            // Exact alarms were allowed or revoked: re-arm the schedule and timer alarms the right way.
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> ReconcileCause.PermissionGranted

            else -> return
        }
        val pending = goAsync()
        scope.launch {
            try {
                inputs.submit(Input.Reconcile(cause))
            } finally {
                pending.finish()
            }
        }
    }
}
