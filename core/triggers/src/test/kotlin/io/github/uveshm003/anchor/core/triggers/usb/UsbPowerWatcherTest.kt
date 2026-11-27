package io.github.uveshm003.anchor.core.triggers.usb

import android.content.Intent
import android.os.BatteryManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.uveshm003.anchor.core.engine.Input
import io.github.uveshm003.anchor.core.engine.SessionInputs
import io.github.uveshm003.anchor.core.engine.TriggerKey
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowLooper

@RunWith(AndroidJUnit4::class)
class UsbPowerWatcherTest {
    private val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    private val submitted = mutableListOf<Input>()
    private val inputs = object : SessionInputs {
        override suspend fun submit(input: Input) {
            submitted += input
        }
    }

    private fun battery(plugged: Int, level: Int = 50) {
        context.sendBroadcast(
            Intent(Intent.ACTION_BATTERY_CHANGED)
                .putExtra(BatteryManager.EXTRA_PLUGGED, plugged)
                .putExtra(BatteryManager.EXTRA_LEVEL, level),
        )
        ShadowLooper.idleMainLooper()
    }

    @Test
    fun submits_only_changes_between_computer_and_not() = runTest {
        val watcher = UsbPowerWatcher(context, inputs, backgroundScope)
        watcher.start()
        watcher.start() // idempotent

        battery(BatteryManager.BATTERY_PLUGGED_USB)
        battery(BatteryManager.BATTERY_PLUGGED_USB, level = 51) // a level change: no new input
        battery(0)
        battery(BatteryManager.BATTERY_PLUGGED_AC)
        battery(BatteryManager.BATTERY_PLUGGED_USB)
        testScheduler.runCurrent()

        assertThat(submitted).containsExactly(
            Input.TriggerStarted(TriggerKey.Usb),
            Input.TriggerEnded(TriggerKey.Usb),
            Input.TriggerStarted(TriggerKey.Usb),
        ).inOrder()

        watcher.stop()
        battery(0)
        testScheduler.runCurrent()
        assertThat(submitted).hasSize(3)
    }

    @Test
    fun only_a_usb_port_counts_as_a_computer() {
        assertThat(UsbPowerWatcher.isComputer(BatteryManager.BATTERY_PLUGGED_USB)).isTrue()
        listOf(
            0,
            BatteryManager.BATTERY_PLUGGED_AC,
            BatteryManager.BATTERY_PLUGGED_WIRELESS,
            BatteryManager.BATTERY_PLUGGED_DOCK,
        ).forEach { assertThat(UsbPowerWatcher.isComputer(it)).isFalse() }
    }
}
