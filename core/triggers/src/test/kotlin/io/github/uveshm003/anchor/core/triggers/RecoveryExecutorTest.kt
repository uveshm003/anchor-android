package io.github.uveshm003.anchor.core.triggers

import android.app.AlarmManager
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.uveshm003.anchor.core.engine.Command
import io.github.uveshm003.anchor.core.engine.ExecutionResult
import io.github.uveshm003.anchor.core.model.SessionId
import io.github.uveshm003.anchor.core.triggers.recovery.RecoveryExecutor
import io.github.uveshm003.anchor.core.triggers.recovery.RecoveryReceiver
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(AndroidJUnit4::class)
class RecoveryExecutorTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val alarms = shadowOf(context.getSystemService(AlarmManager::class.java))

    @Test
    fun schedules_on_the_monotonic_clock_and_cancels() = runTest {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        val executor = RecoveryExecutor(context)

        assertThat(
            executor.execute(Command.ScheduleRecovery("r", SessionId(7), 86_400_000)),
        ).isEqualTo(ExecutionResult.Done)
        val alarm = checkNotNull(alarms.nextScheduledAlarm)
        assertThat(alarm.type).isEqualTo(AlarmManager.ELAPSED_REALTIME_WAKEUP)
        assertThat(alarm.triggerAtTime).isEqualTo(86_400_000)
        assertThat(
            shadowOf(alarm.operation).savedIntent.getLongExtra(RecoveryReceiver.EXTRA_SESSION_ID, -1),
        ).isEqualTo(7)

        executor.execute(Command.CancelRecovery("c", SessionId(7)))
        assertThat(alarms.scheduledAlarms).isEmpty()
        assertThat(executor.execute(Command.ClearAppPolicy("x"))).isInstanceOf(ExecutionResult.Failed::class.java)
    }
}
