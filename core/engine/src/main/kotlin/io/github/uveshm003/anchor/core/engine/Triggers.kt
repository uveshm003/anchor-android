package io.github.uveshm003.anchor.core.engine

import io.github.uveshm003.anchor.core.model.Mode
import io.github.uveshm003.anchor.core.model.ModeId
import io.github.uveshm003.anchor.core.model.PlaceId
import io.github.uveshm003.anchor.core.model.SessionSource
import io.github.uveshm003.anchor.core.model.SpaceId
import io.github.uveshm003.anchor.core.model.StartTrigger
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** An automatic trigger that is currently true (reported by an adapter in `core:triggers`). */
@Serializable
sealed interface TriggerKey {
    /** Whether [trigger], one of the start triggers of the mode [modeId], fires on this key. */
    fun matches(trigger: StartTrigger, modeId: ModeId): Boolean

    /** A mode's schedule window is open. */
    @Serializable
    @SerialName("schedule")
    data class Schedule(val modeId: ModeId) : TriggerKey {
        override fun matches(trigger: StartTrigger, modeId: ModeId) =
            modeId == this.modeId && trigger is StartTrigger.Schedule
    }

    @Serializable
    @SerialName("place")
    data class Place(val placeId: PlaceId) : TriggerKey {
        override fun matches(trigger: StartTrigger, modeId: ModeId) =
            trigger is StartTrigger.Place && trigger.placeId == placeId
    }

    @Serializable
    @SerialName("driving")
    data object Driving : TriggerKey {
        override fun matches(trigger: StartTrigger, modeId: ModeId) = trigger == StartTrigger.Driving
    }

    /** A Bluetooth device (car, earbuds) is connected. */
    @Serializable
    @SerialName("device")
    data class Device(val address: String) : TriggerKey {
        override fun matches(trigger: StartTrigger, modeId: ModeId) =
            trigger is StartTrigger.BluetoothDevice && trigger.address.equals(address, ignoreCase = true)
    }

    @Serializable
    @SerialName("space")
    data class Space(val spaceId: SpaceId) : TriggerKey {
        override fun matches(trigger: StartTrigger, modeId: ModeId) =
            trigger is StartTrigger.Space && trigger.spaceId == spaceId
    }

    /** Connected to Wi-Fi [ssid] through access point [bssid] (null when unknown). */
    @Serializable
    @SerialName("wifi")
    data class Wifi(val ssid: String, val bssid: String? = null) : TriggerKey {
        override fun matches(trigger: StartTrigger, modeId: ModeId) = trigger is StartTrigger.WifiNetwork &&
            trigger.ssid == ssid && (trigger.bssid == null || trigger.bssid.equals(bssid, ignoreCase = true))
    }

    /** Plugged into a computer over USB. */
    @Serializable
    @SerialName("usb")
    data object Usb : TriggerKey {
        override fun matches(trigger: StartTrigger, modeId: ModeId) = trigger == StartTrigger.UsbConnected
    }
}

/** Which modes an active trigger can start, and which one wins (pure; ADR 0003). */
object Triggers {
    data class Candidate(val key: TriggerKey, val mode: Mode)

    fun matches(key: TriggerKey, mode: Mode): Boolean = mode.startTriggers.any { key.matches(it, mode.id) }

    /** The highest-priority mode (lowest number, then lowest id) that any of [keys] can start. */
    fun best(keys: Set<TriggerKey>, modes: List<Mode>): Candidate? = keys
        .flatMap { key -> modes.filter { matches(key, it) }.map { Candidate(key, it) } }
        .minWithOrNull(compareBy({ it.mode.priority }, { it.mode.id.value }, { it.key.toString() }))

    fun source(key: TriggerKey): SessionSource = when (key) {
        is TriggerKey.Schedule -> SessionSource.Schedule
        is TriggerKey.Place -> SessionSource.Place
        TriggerKey.Driving -> SessionSource.Driving
        is TriggerKey.Device -> SessionSource.BluetoothDevice
        is TriggerKey.Space -> SessionSource.Space
        is TriggerKey.Wifi -> SessionSource.Wifi
        TriggerKey.Usb -> SessionSource.Usb
    }
}
