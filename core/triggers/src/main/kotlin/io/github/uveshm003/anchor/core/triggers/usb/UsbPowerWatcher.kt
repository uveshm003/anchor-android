package io.github.uveshm003.anchor.core.triggers.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.uveshm003.anchor.core.common.dispatchers.ApplicationScope
import io.github.uveshm003.anchor.core.engine.Input
import io.github.uveshm003.anchor.core.engine.ProcessWatcher
import io.github.uveshm003.anchor.core.engine.SessionInputs
import io.github.uveshm003.anchor.core.engine.TriggerKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * "Connected to a computer": the phone is charging from a USB port (ADR 0010). `ACTION_POWER_CONNECTED` no longer
 * reaches manifest receivers and there is no public device-mode USB signal, so this listens to the sticky
 * `ACTION_BATTERY_CHANGED` while the blocker service keeps the process alive. Its first delivery is the current
 * state, which also clears a stale trigger after the process was restarted. Only changes are submitted.
 */
@Singleton
internal class UsbPowerWatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val inputs: SessionInputs,
    @ApplicationScope private val scope: CoroutineScope,
) : ProcessWatcher {
    private var registered = false
    private var usb: Boolean? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            onPlugged(intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0))
        }
    }

    @Synchronized
    override fun start() {
        if (registered) return
        registered = true
        // System broadcasts are delivered to non-exported receivers.
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    @Synchronized
    override fun stop() {
        if (!registered) return
        registered = false
        usb = null
        context.unregisterReceiver(receiver)
    }

    @Synchronized
    private fun onPlugged(plugged: Int) {
        val now = isComputer(plugged)
        if (now == usb) return
        usb = now
        val input = if (now) Input.TriggerStarted(TriggerKey.Usb) else Input.TriggerEnded(TriggerKey.Usb)
        scope.launch { inputs.submit(input) }
    }

    companion object {
        /**
         * `BATTERY_PLUGGED_USB` ("Power source is a USB port"); AC, wireless and dock are not a computer. Some USB
         * wall adapters without charger detection also report USB, which the editor says.
         */
        fun isComputer(plugged: Int): Boolean = plugged == BatteryManager.BATTERY_PLUGGED_USB
    }
}
