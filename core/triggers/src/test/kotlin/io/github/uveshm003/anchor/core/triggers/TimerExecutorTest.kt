package io.github.uveshm003.anchor.core.triggers

import android.app.AlarmManager
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.uveshm003.anchor.core.engine.Command
import io.github.uveshm003.anchor.core.engine.ExecutionResult
import io.github.uveshm003.anchor.core.model.SessionId
import io.github.uveshm003.anchor.core.triggers.timer.TimerExecutor
import io.github.uveshm003.anchor.core.triggers.timer.TimerReceiver
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(AndroidJUnit4::class)
class TimerExecutorTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val alarms = shadowOf(context.getSystemService(AlarmManager::class.java))
    private val at = Instant.parse("2026-11-23T10:30:00Z")

    @Test
    fun schedules_exact_when_allowed_and_cancels() = runTest {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        val executor = TimerExecutor(context)

        assertThat(executor.execute(Command.ScheduleTimer("t", SessionId(4), at))).isEqualTo(ExecutionResult.Done)

        val alarm = checkNotNull(alarms.nextScheduledAlarm)
        assertThat(alarm.triggerAtTime).isEqualTo(at.toEpochMilli())
        assertThat(alarm.isAllowWhileIdle).isTrue()
        assertThat(shadowOf(alarm.operation).savedIntent.getLongExtra(TimerReceiver.EXTRA_SESSION_ID, -1)).isEqualTo(4)

        executor.execute(Command.CancelTimer("c", SessionId(4)))
        assertThat(alarms.scheduledAlarms).isEmpty()
    }

    @Test
    fun falls_back_to_inexact_without_the_exact_alarm_permission() = runTest {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        val executor = TimerExecutor(context)

        executor.execute(Command.ScheduleTimer("t", SessionId(5), at))

        assertThat(alarms.nextScheduledAlarm?.triggerAtTime).isEqualTo(at.toEpochMilli())
        assertThat(executor.execute(Command.ClearAppPolicy("x"))).isInstanceOf(ExecutionResult.Failed::class.java)
    }
}
