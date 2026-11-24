package io.github.uveshm003.anchor.service.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.uveshm003.anchor.core.engine.Command
import io.github.uveshm003.anchor.core.engine.CommandExecutor
import io.github.uveshm003.anchor.core.engine.ExecutionResult
import io.github.uveshm003.anchor.core.model.ExitRule
import java.time.Clock
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

/**
 * The ongoing "session is on" notification: mode, a chronometer from the start and how it ends; tapping opens
 * Anchor. Low importance (silent), no foreground service. Without the notification permission it does nothing.
 * Not a Live Update: a focus session is ongoing and user-started but doesn't need watching throughout
 * (https://developer.android.com/develop/ui/views/notifications/live-update, checked 2026-11-24).
 */
class SessionNotificationExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
) : CommandExecutor {
    override val handles = setOf(Command.ShowSessionNotification::class, Command.HideSessionNotification::class)

    private val manager = NotificationManagerCompat.from(context)

    override suspend fun execute(command: Command): ExecutionResult {
        when (command) {
            is Command.ShowSessionNotification -> show(command)
            is Command.HideSessionNotification -> manager.cancel(NOTIFICATION_ID)
            else -> return ExecutionResult.Failed("Not a notification command: ${command::class.simpleName}")
        }
        return ExecutionResult.Done
    }

    private fun show(command: Command.ShowSessionNotification) {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.service_notifications_channel),
                NotificationManager.IMPORTANCE_LOW,
            )
                .apply { description = context.getString(R.string.service_notifications_channel_description) },
        )
        val open = context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.service_notifications_ic_anchor)
            .setContentTitle(context.getString(R.string.service_notifications_title, command.modeName))
            .setContentText(howItEnds(command))
            .setWhen(command.startedAt.toEpochMilli())
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(open)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun howItEnds(command: Command.ShowSessionNotification): String = when (command.exitRule) {
        is ExitRule.RequiresAnchor ->
            command.requiredAnchorName
                ?.let { context.getString(R.string.service_notifications_ends_specific_anchor, it) }
                ?: context.getString(R.string.service_notifications_ends_any_anchor)

        is ExitRule.TimerEnds -> context.getString(
            R.string.service_notifications_ends_at,
            DateTimeFormatter.ofLocalizedTime(
                FormatStyle.SHORT,
            ).format(checkNotNull(command.endsAt).atZone(clock.zone)),
        )

        ExitRule.TriggerEnds -> context.getString(R.string.service_notifications_ends_trigger)
    }

    companion object {
        const val CHANNEL_ID = "active_session"
        const val NOTIFICATION_ID = 1
    }
}
