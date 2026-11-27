package io.github.uveshm003.anchor.core.triggers

import android.app.AlarmManager
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.uveshm003.anchor.core.engine.Command
import io.github.uveshm003.anchor.core.engine.ExecutionResult
import io.github.uveshm003.anchor.core.triggers.schedule.ScheduleWakeExecutor
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(AndroidJUnit4::class)
class ScheduleWakeTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val alarms = shadowOf(context.getSystemService(AlarmManager::class.java))
    private val at = Instant.parse("2026-11-27T16:30:00Z")

    @Test
    fun exact_when_allowed_windowed_otherwise_and_cancel_on_null() = runTest {
        val executor = ScheduleWakeExecutor(context)

        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        executor.execute(Command.ScheduleWake("w", at))
        assertThat(alarms.nextScheduledAlarm?.triggerAtTime).isEqualTo(at.toEpochMilli())

        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        executor.execute(Command.ScheduleWake("w2", at))
        // Inexact: setWindow(at, WINDOW_MS); the shadow records the start of the window.
        assertThat(alarms.nextScheduledAlarm?.triggerAtTime).isEqualTo(at.toEpochMilli())

        executor.execute(Command.ScheduleWake("w3", null))
        assertThat(alarms.scheduledAlarms).isEmpty()
        assertThat(executor.execute(Command.ClearAppPolicy("x"))).isInstanceOf(ExecutionResult.Failed::class.java)
    }
}
