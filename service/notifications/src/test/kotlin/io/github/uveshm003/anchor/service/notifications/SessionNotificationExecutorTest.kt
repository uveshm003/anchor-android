package io.github.uveshm003.anchor.service.notifications

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.uveshm003.anchor.core.common.time.TestClock
import io.github.uveshm003.anchor.core.engine.Command
import io.github.uveshm003.anchor.core.engine.ExecutionResult
import io.github.uveshm003.anchor.core.model.ExitRule
import io.github.uveshm003.anchor.core.model.SessionId
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class SessionNotificationExecutorTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val start = Instant.parse("2026-11-24T10:00:00Z")
    private val show = Command.ShowSessionNotification(
        "s",
        SessionId(1),
        "Deep Work",
        start,
        ExitRule.RequiresAnchor(),
        null,
        "Desk",
    )

    @Test
    fun shows_an_ongoing_low_importance_notification_and_removes_it() = runTest {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName)
        shadowOf(context.packageManager).addResolveInfoForIntent(
            launcher,
            ResolveInfo().apply {
                activityInfo =
                    ActivityInfo().apply {
                        packageName = context.packageName
                        name = "Main"
                    }
            },
        )
        val executor = SessionNotificationExecutor(context, TestClock())

        assertThat(executor.execute(show)).isEqualTo(ExecutionResult.Done)
        val posted = shadowOf(manager).allNotifications.single()
        val shadow = shadowOf(posted)
        assertThat(shadow.contentTitle).isEqualTo("Deep Work is on")
        assertThat(shadow.contentText).isEqualTo("Tap your Desk anchor to end it")
        assertThat(posted.flags and android.app.Notification.FLAG_ONGOING_EVENT).isNotEqualTo(0)
        assertThat(posted.`when`).isEqualTo(start.toEpochMilli())
        assertThat(posted.contentIntent.isImmutable).isTrue()
        assertThat(manager.getNotificationChannel(SessionNotificationExecutor.CHANNEL_ID).importance)
            .isEqualTo(NotificationManager.IMPORTANCE_LOW)

        executor.execute(Command.HideSessionNotification("h"))
        assertThat(shadowOf(manager).allNotifications).isEmpty()
    }

    @Test
    fun does_nothing_without_the_permission_and_rejects_other_commands() = runTest {
        shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val executor = SessionNotificationExecutor(context, TestClock())

        assertThat(executor.execute(show.copy(exitRule = ExitRule.TimerEnds(30), endsAt = start.plusSeconds(1800))))
            .isEqualTo(ExecutionResult.Done)
        assertThat(shadowOf(manager).allNotifications).isEmpty()
        assertThat(executor.execute(Command.ClearAppPolicy("x"))).isInstanceOf(ExecutionResult.Failed::class.java)
    }
}
