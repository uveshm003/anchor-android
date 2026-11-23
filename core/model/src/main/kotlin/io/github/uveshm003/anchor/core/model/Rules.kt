package io.github.uveshm003.anchor.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** What can start a mode. A mode may have several. */
@Serializable
sealed interface StartTrigger {
    /** Press and hold in the app. */
    @Serializable
    @SerialName("hold")
    data object Hold : StartTrigger

    /** Tap a paired anchor. */
    @Serializable
    @SerialName("anchor")
    data object AnchorTap : StartTrigger

    /** Days (ISO 1 = Monday … 7 = Sunday) and a local time window in minutes after midnight; may wrap midnight. */
    @Serializable
    @SerialName("schedule")
    data class Schedule(val days: Set<Int>, val startMinute: Int, val endMinute: Int) : StartTrigger {
        init {
            require(days.isNotEmpty() && days.all { it in 1..DAYS_PER_WEEK }) { "Days must be ISO 1..7: $days" }
            require(startMinute in MINUTES_RANGE && endMinute in MINUTES_RANGE) { "Minutes must be 0..1439" }
            require(startMinute != endMinute) { "A schedule window must not be empty" }
        }
    }

    @Serializable
    @SerialName("place")
    data class Place(val placeId: PlaceId) : StartTrigger

    @Serializable
    @SerialName("driving")
    data object Driving : StartTrigger

    @Serializable
    @SerialName("bluetooth")
    data class BluetoothDevice(val address: String) : StartTrigger {
        init {
            require(MAC_ADDRESS.matches(address)) { "Not a Bluetooth MAC address: $address" }
        }
    }

    @Serializable
    @SerialName("space")
    data class Space(val spaceId: SpaceId) : StartTrigger

    /** Connected to a Wi-Fi network, optionally a specific access point. */
    @Serializable
    @SerialName("wifi")
    data class WifiNetwork(val ssid: String, val bssid: String? = null) : StartTrigger {
        init {
            require(ssid.isNotBlank()) { "SSID must not be blank" }
            require(bssid == null || MAC_ADDRESS.matches(bssid)) { "Not a BSSID: $bssid" }
        }
    }

    /** Connected to a computer over USB. */
    @Serializable
    @SerialName("usb")
    data object UsbConnected : StartTrigger
}

/** What ends a session. */
@Serializable
sealed interface ExitRule {
    /** Tap an anchor: any paired anchor when [anchorId] is null, otherwise that one. */
    @Serializable
    @SerialName("anchor")
    data class RequiresAnchor(val anchorId: AnchorId? = null) : ExitRule

    /** Ends by itself after [minutes]. */
    @Serializable
    @SerialName("timer")
    data class TimerEnds(val minutes: Int) : ExitRule {
        init {
            require(minutes > 0) { "Timer must be > 0 minutes, was $minutes" }
        }
    }

    /** Ends when the trigger that started it ends (leave the place, disconnect the car, …). */
    @Serializable
    @SerialName("trigger")
    data object TriggerEnds : ExitRule
}

private const val DAYS_PER_WEEK = 7
private val MINUTES_RANGE = 0 until 24 * 60
private val MAC_ADDRESS = Regex("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$")
