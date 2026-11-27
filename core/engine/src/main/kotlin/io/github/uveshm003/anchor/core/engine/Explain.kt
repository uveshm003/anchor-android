package io.github.uveshm003.anchor.core.engine

import io.github.uveshm003.anchor.core.model.Mode
import io.github.uveshm003.anchor.core.model.StartTrigger
import java.time.Instant

/** Why a mode is or isn't running right now, as data for the UI (#56). */
sealed interface ModeStatus {
    data object Running : ModeStatus

    /** Its trigger is on, but [reason] keeps it from starting. */
    data class Waiting(val reason: WaitReason) : ModeStatus

    /** It starts by itself at [at] (next schedule window). */
    data class StartsAt(val at: Instant) : ModeStatus

    /** No automatic trigger is on: it starts when the user holds or taps an anchor. */
    data object Manual : ModeStatus
}

sealed interface WaitReason {
    /** Automatic triggers never override a manual or anchor session. */
    data class ManualSession(val modeName: String) : WaitReason

    /** Another automatic mode is running and wins (higher priority, or it can't be preempted). */
    data class OtherMode(val modeName: String) : WaitReason

    /** It was ended early (e.g. emergency unlock); it waits until its trigger turns off and on again. */
    data object UsedUp : WaitReason
}

/** A permission a start trigger relies on. */
enum class TriggerPermission {
    /** Optional: without it schedules may run up to 10 minutes late. */
    ExactAlarm,
    Location,
    NearbyDevices,
    ActivityRecognition,
}

object Explain {
    /** The permissions [mode]'s start triggers rely on. */
    fun needs(mode: Mode): Set<TriggerPermission> = mode.startTriggers.mapNotNullTo(mutableSetOf()) {
        when (it) {
            StartTrigger.Hold, StartTrigger.AnchorTap, StartTrigger.UsbConnected -> null

            is StartTrigger.Schedule -> TriggerPermission.ExactAlarm

            // Android treats the SSID/BSSID of the current network as location data.
            is StartTrigger.Place, is StartTrigger.WifiNetwork -> TriggerPermission.Location

            StartTrigger.Driving -> TriggerPermission.ActivityRecognition

            is StartTrigger.BluetoothDevice, is StartTrigger.Space -> TriggerPermission.NearbyDevices
        }
    }

    fun status(mode: Mode, state: EngineState, env: Environment): ModeStatus {
        val active = state.active
        val triggered = state.activeTriggers.any { Triggers.matches(it, mode) }
        val spent = state.spentTriggers.any { Triggers.matches(it, mode) }
        val nextStart = nextScheduleStart(mode, env)
        return when {
            active?.mode?.id == mode.id -> ModeStatus.Running

            triggered && active != null && active.trigger == null -> ModeStatus.Waiting(
                WaitReason.ManualSession(active.session.modeName),
            )

            triggered && active != null -> ModeStatus.Waiting(WaitReason.OtherMode(active.mode.name))

            spent -> ModeStatus.Waiting(WaitReason.UsedUp)

            nextStart != null -> ModeStatus.StartsAt(nextStart)

            else -> ModeStatus.Manual
        }
    }

    private fun nextScheduleStart(mode: Mode, env: Environment): Instant? = mode.startTriggers
        .filterIsInstance<StartTrigger.Schedule>()
        .flatMap { Schedules.windows(it, env.now, env.zone) }
        .map { it.start }
        .filter { it.isAfter(env.now) }
        .minOrNull()
}
