package io.github.uveshm003.anchor.core.engine

import com.google.common.truth.Truth.assertThat
import io.github.uveshm003.anchor.core.engine.Fixtures.activeIn
import io.github.uveshm003.anchor.core.engine.Fixtures.env
import io.github.uveshm003.anchor.core.engine.Fixtures.work
import io.github.uveshm003.anchor.core.model.ExitRule
import io.github.uveshm003.anchor.core.model.Mode
import io.github.uveshm003.anchor.core.model.ModeId
import io.github.uveshm003.anchor.core.model.StartTrigger
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Test

class ExplainTest {
    private val night = Mode(
        ModeId(11),
        "Night",
        priority = 0,
        startTriggers = setOf(StartTrigger.Schedule(setOf(1, 2, 3, 4, 5, 6, 7), 22 * 60, 7 * 60)),
        exitRule = ExitRule.TriggerEnds,
    )
    private val commute =
        Mode(
            ModeId(10),
            "Commute",
            priority = 1,
            startTriggers = setOf(StartTrigger.Driving),
            exitRule = ExitRule.TriggerEnds,
        )
    private val noon = env.copy(
        now = Instant.parse("2026-11-23T12:00:00Z"),
        zone = ZoneOffset.UTC,
        modes = listOf(work, night, commute),
    )

    @Test
    fun explains_running_waiting_scheduled_and_manual_modes() {
        val manual = activeIn(work).copy(activeTriggers = setOf(TriggerKey.Driving))
        assertThat(Explain.status(work, manual, noon)).isEqualTo(ModeStatus.Running)
        assertThat(
            Explain.status(commute, manual, noon),
        ).isEqualTo(ModeStatus.Waiting(WaitReason.ManualSession("Work")))
        assertThat(
            Explain.status(night, manual, noon),
        ).isEqualTo(ModeStatus.StartsAt(Instant.parse("2026-11-23T22:00:00Z")))

        val nightRunning = Engine.reduce(EngineState(), Input.TriggerStarted(TriggerKey.Schedule(night.id)), noon).state
            .copy(activeTriggers = setOf(TriggerKey.Schedule(night.id), TriggerKey.Driving))
        assertThat(
            Explain.status(commute, nightRunning, noon),
        ).isEqualTo(ModeStatus.Waiting(WaitReason.OtherMode("Night")))

        val spent = EngineState(spentTriggers = setOf(TriggerKey.Driving))
        assertThat(Explain.status(commute, spent, noon)).isEqualTo(ModeStatus.Waiting(WaitReason.UsedUp))
        assertThat(Explain.status(commute, EngineState(), noon)).isEqualTo(ModeStatus.Manual)
    }

    @Test
    fun lists_the_permissions_a_modes_triggers_rely_on() {
        assertThat(Explain.needs(work)).isEmpty()
        assertThat(Explain.needs(night)).containsExactly(TriggerPermission.ExactAlarm)
        val everything = commute.copy(
            startTriggers = setOf(
                StartTrigger.Driving,
                StartTrigger.Place(io.github.uveshm003.anchor.core.model.PlaceId(1)),
                StartTrigger.BluetoothDevice("AA:BB:CC:DD:EE:FF"),
                StartTrigger.Space(io.github.uveshm003.anchor.core.model.SpaceId(1)),
                StartTrigger.WifiNetwork("Home"),
                StartTrigger.UsbConnected,
            ),
        )
        assertThat(Explain.needs(everything)).containsExactly(
            TriggerPermission.ActivityRecognition,
            TriggerPermission.Location,
            TriggerPermission.NearbyDevices,
        )
    }
}
