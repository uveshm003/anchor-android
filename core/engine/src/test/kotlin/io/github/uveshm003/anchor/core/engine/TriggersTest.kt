package io.github.uveshm003.anchor.core.engine

import com.google.common.truth.Truth.assertThat
import io.github.uveshm003.anchor.core.engine.Fixtures.activeIn
import io.github.uveshm003.anchor.core.engine.Fixtures.desk
import io.github.uveshm003.anchor.core.engine.Fixtures.env
import io.github.uveshm003.anchor.core.engine.Fixtures.work
import io.github.uveshm003.anchor.core.model.ExitRule
import io.github.uveshm003.anchor.core.model.Mode
import io.github.uveshm003.anchor.core.model.ModeId
import io.github.uveshm003.anchor.core.model.PlaceId
import io.github.uveshm003.anchor.core.model.SessionEnd
import io.github.uveshm003.anchor.core.model.SessionSource
import io.github.uveshm003.anchor.core.model.SpaceId
import io.github.uveshm003.anchor.core.model.StartTrigger
import kotlinx.serialization.json.Json
import org.junit.Test

class TriggersTest {
    private val night = Mode(
        ModeId(11),
        "Night",
        priority = 0,
        startTriggers = setOf(StartTrigger.Schedule(setOf(1), 1320, 420)),
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
    private val office =
        Mode(ModeId(12), "Office", priority = 2, startTriggers = setOf(StartTrigger.WifiNetwork("Office")))
    private val triggered = env.copy(modes = env.modes + listOf(night, commute, office))

    private fun EngineState.reduce(input: Input) = Engine.reduce(this, input, triggered)

    @Test
    fun an_automatic_trigger_starts_its_mode_and_ending_it_ends_only_that_session() {
        val started = EngineState().reduce(Input.TriggerStarted(TriggerKey.Driving))
        val session = (started.outcome as Outcome.Started).session
        assertThat(session.modeId).isEqualTo(commute.id)
        assertThat(session.startedBy).isEqualTo(SessionSource.Driving)
        assertThat(started.state.active?.trigger).isEqualTo(TriggerKey.Driving)

        // Another trigger ending doesn't touch it.
        assertThat(started.state.reduce(Input.TriggerEnded(TriggerKey.Usb)).outcome).isEqualTo(Outcome.TriggersChanged)

        val ended = started.state.reduce(Input.TriggerEnded(TriggerKey.Driving))
        assertThat((ended.outcome as Outcome.Ended).session.endedBy).isEqualTo(SessionEnd.TriggerEnded)
        assertThat(ended.state.activeTriggers).isEmpty()
    }

    @Test
    fun automatic_triggers_never_override_a_manual_session_and_start_after_it_ends() {
        val manual = activeIn(work)
        val noted = manual.reduce(Input.TriggerStarted(TriggerKey.Driving))
        assertThat(noted.outcome).isEqualTo(Outcome.TriggersChanged)
        assertThat(noted.state.active?.mode).isEqualTo(work)
        assertThat(noted.commands).isEmpty()

        val switched = noted.state.reduce(Input.AnchorTapped(desk.hash)).outcome as Outcome.Switched
        assertThat(switched.ended.endedBy).isEqualTo(SessionEnd.AnchorTap)
        assertThat(switched.started.modeId).isEqualTo(commute.id)
    }

    @Test
    fun the_highest_priority_trigger_wins_and_the_next_one_resumes() {
        val driving = EngineState().reduce(Input.TriggerStarted(TriggerKey.Driving)).state
        val preempted = driving.reduce(Input.TriggerStarted(TriggerKey.Schedule(night.id)))
        val switched = preempted.outcome as Outcome.Switched
        assertThat(switched.ended.endedBy).isEqualTo(SessionEnd.Preempted)
        assertThat(switched.started.modeId).isEqualTo(night.id)
        assertThat(preempted.state.activeTriggers).containsExactly(TriggerKey.Driving, TriggerKey.Schedule(night.id))

        // A lower-priority trigger doesn't replace it.
        assertThat(preempted.state.reduce(Input.TriggerStarted(TriggerKey.Wifi("Office"))).outcome)
            .isEqualTo(Outcome.TriggersChanged)

        val resumed = preempted.state.reduce(
            Input.TriggerEnded(TriggerKey.Schedule(night.id)),
        ).outcome as Outcome.Switched
        assertThat(resumed.started.modeId).isEqualTo(commute.id)
    }

    @Test
    fun a_strict_automatic_session_outlives_its_trigger_and_is_never_preempted() {
        val atOffice = EngineState().reduce(Input.TriggerStarted(TriggerKey.Wifi("Office", "00:11:22:33:44:55"))).state
        assertThat(atOffice.active?.mode).isEqualTo(office)

        val left = atOffice.reduce(Input.TriggerEnded(TriggerKey.Wifi("Office", "00:11:22:33:44:55")))
        assertThat(left.outcome).isEqualTo(Outcome.TriggersChanged)
        assertThat(left.state.active?.mode).isEqualTo(office)

        assertThat(
            left.state.reduce(Input.TriggerStarted(TriggerKey.Driving)).outcome,
        ).isEqualTo(Outcome.TriggersChanged)
    }

    @Test
    fun emergency_unlock_uses_up_every_trigger_so_nothing_restarts() {
        val both = EngineState().reduce(Input.TriggerStarted(TriggerKey.Driving)).state
            .reduce(Input.TriggerStarted(TriggerKey.Wifi("Office"))).state

        val unlocked = both.reduce(Input.EmergencyUnlock)

        assertThat(unlocked.outcome).isInstanceOf(Outcome.Ended::class.java)
        assertThat(unlocked.state.activeTriggers).isEmpty()
        assertThat(unlocked.state.active).isNull()
    }

    @Test
    fun triggers_match_their_start_triggers() {
        val car = Mode(ModeId(20), "Car", startTriggers = setOf(StartTrigger.BluetoothDevice("AA:BB:CC:DD:EE:FF")))
        val ap = Mode(ModeId(21), "AP", startTriggers = setOf(StartTrigger.WifiNetwork("Home", "00:11:22:33:44:55")))
        val gym = Mode(ModeId(22), "Gym", startTriggers = setOf(StartTrigger.Place(PlaceId(3))))
        val bed = Mode(ModeId(23), "Bed", startTriggers = setOf(StartTrigger.Space(SpaceId(4))))
        val desk = Mode(ModeId(24), "Desk", startTriggers = setOf(StartTrigger.UsbConnected))

        assertThat(Triggers.matches(TriggerKey.Device("aa:bb:cc:dd:ee:ff"), car)).isTrue()
        assertThat(Triggers.matches(TriggerKey.Wifi("Home", "00:11:22:33:44:55"), ap)).isTrue()
        assertThat(Triggers.matches(TriggerKey.Wifi("Home", "99:11:22:33:44:55"), ap)).isFalse()
        assertThat(Triggers.matches(TriggerKey.Wifi("Office"), office)).isTrue()
        assertThat(Triggers.matches(TriggerKey.Place(PlaceId(3)), gym)).isTrue()
        assertThat(Triggers.matches(TriggerKey.Place(PlaceId(9)), gym)).isFalse()
        assertThat(Triggers.matches(TriggerKey.Space(SpaceId(4)), bed)).isTrue()
        assertThat(Triggers.matches(TriggerKey.Usb, desk)).isTrue()
        assertThat(Triggers.matches(TriggerKey.Schedule(night.id), night)).isTrue()
        assertThat(Triggers.matches(TriggerKey.Schedule(commute.id), night)).isFalse()
        assertThat(Triggers.matches(TriggerKey.Driving, work)).isFalse()

        val sources = listOf(
            TriggerKey.Schedule(night.id),
            TriggerKey.Place(PlaceId(1)),
            TriggerKey.Driving,
            TriggerKey.Device("x"),
            TriggerKey.Space(SpaceId(1)),
            TriggerKey.Wifi("w"),
            TriggerKey.Usb,
        ).map(Triggers::source)
        assertThat(sources).containsExactlyElementsIn(
            listOf(
                SessionSource.Schedule,
                SessionSource.Place,
                SessionSource.Driving,
                SessionSource.BluetoothDevice,
                SessionSource.Space,
                SessionSource.Wifi,
                SessionSource.Usb,
            ),
        ).inOrder()
    }

    @Test
    fun active_triggers_survive_a_restart() {
        val state = EngineState().reduce(Input.TriggerStarted(TriggerKey.Driving)).state
        assertThat(Json.decodeFromString<EngineState>(Json.encodeToString(state))).isEqualTo(state)
    }

    @Test
    fun a_trigger_already_on_starts_a_mode_that_just_gained_it() {
        // USB plugged in while no mode used it: noted, nothing starts.
        val plugged = EngineState().reduce(Input.TriggerStarted(TriggerKey.Usb))
        assertThat(plugged.outcome).isEqualTo(Outcome.TriggersChanged)
        assertThat(plugged.state.active).isNull()

        // The user adds "Connected to a computer" to a mode; the next reconcile starts it.
        val pc = Mode(
            ModeId(13),
            "At the PC",
            priority = 3,
            startTriggers = setOf(StartTrigger.UsbConnected),
            exitRule = ExitRule.TriggerEnds,
        )
        val withPc = triggered.copy(modes = triggered.modes + pc)
        val saved = Engine.reduce(plugged.state, Input.Reconcile(ReconcileCause.AppStart), withPc)

        assertThat(saved.outcome).isInstanceOf(Outcome.Started::class.java)
        assertThat(saved.state.active?.mode?.id).isEqualTo(pc.id)
        assertThat(saved.state.active?.trigger).isEqualTo(TriggerKey.Usb)
    }
}
