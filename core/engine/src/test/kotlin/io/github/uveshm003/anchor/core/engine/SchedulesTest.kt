package io.github.uveshm003.anchor.core.engine

import com.google.common.truth.Truth.assertThat
import io.github.uveshm003.anchor.core.engine.Fixtures.env
import io.github.uveshm003.anchor.core.model.ExitRule
import io.github.uveshm003.anchor.core.model.Mode
import io.github.uveshm003.anchor.core.model.ModeId
import io.github.uveshm003.anchor.core.model.SessionEnd
import io.github.uveshm003.anchor.core.model.SessionSource
import io.github.uveshm003.anchor.core.model.StartTrigger
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Test

class SchedulesTest {
    private val london = ZoneId.of("Europe/London")
    private val kolkata = ZoneId.of("Asia/Kolkata")
    private val newYork = ZoneId.of("America/New_York")

    /** Weeknights 22:00–07:00 (crosses midnight). */
    private val nights = StartTrigger.Schedule(setOf(1, 2, 3, 4, 5), startMinute = 22 * 60, endMinute = 7 * 60)

    /** Weekdays 09:00–17:00. */
    private val workday = StartTrigger.Schedule(setOf(1, 2, 3, 4, 5), startMinute = 9 * 60, endMinute = 17 * 60)

    private fun at(local: String, zone: ZoneId): Instant = LocalDateTime.parse(local).atZone(zone).toInstant()

    @Test
    fun windows_open_on_listed_days_and_cross_midnight() {
        // 2026-11-23 is a Monday.
        assertThat(Schedules.isOpen(workday, at("2026-11-23T09:00", kolkata), kolkata)).isTrue()
        assertThat(Schedules.isOpen(workday, at("2026-11-23T17:00", kolkata), kolkata)).isFalse()
        assertThat(Schedules.isOpen(workday, at("2026-11-28T10:00", kolkata), kolkata)).isFalse() // Saturday
        // Friday night's window runs into Saturday morning; Saturday night has none.
        assertThat(Schedules.isOpen(nights, at("2026-11-28T06:59", kolkata), kolkata)).isTrue()
        assertThat(Schedules.isOpen(nights, at("2026-11-28T23:00", kolkata), kolkata)).isFalse()
        assertThat(
            Schedules.nextChange(nights, at("2026-11-23T12:00", kolkata), kolkata),
        ).isEqualTo(at("2026-11-23T22:00", kolkata))
        assertThat(
            Schedules.nextChange(nights, at("2026-11-23T23:00", kolkata), kolkata),
        ).isEqualTo(at("2026-11-24T07:00", kolkata))
    }

    @Test
    fun local_times_hold_across_dst_changes() {
        // London falls back on 2026-10-25: the window still opens at 22:00 and closes at 07:00 local time.
        val sunday = StartTrigger.Schedule(setOf(6), startMinute = 22 * 60, endMinute = 7 * 60) // Saturday night
        val closes = Schedules.nextChange(sunday, at("2026-10-24T23:00", london), london)
        assertThat(closes).isEqualTo(at("2026-10-25T07:00", london))
        assertThat(java.time.Duration.between(at("2026-10-24T22:00", london), closes!!).toHours()).isEqualTo(10)

        // New York springs forward on 2026-03-08 at 02:00: a 02:30 start moves forward with the clock.
        val early = StartTrigger.Schedule(setOf(7), startMinute = 2 * 60 + 30, endMinute = 5 * 60)
        assertThat(
            Schedules.nextChange(early, at("2026-03-08T00:00", newYork), newYork),
        ).isEqualTo(at("2026-03-08T03:30", newYork))
    }

    @Test
    fun the_same_schedule_follows_a_time_zone_change() {
        val now = Instant.parse("2026-11-23T17:00:00Z") // 22:30 in Kolkata, 17:00 in London
        assertThat(Schedules.isOpen(nights, now, kolkata)).isTrue()
        assertThat(Schedules.isOpen(nights, now, london)).isFalse()
    }

    private val sleep = Mode(ModeId(30), "Sleep", startTriggers = setOf(nights), exitRule = ExitRule.TriggerEnds)

    private fun tick(state: EngineState, local: String) = Engine.reduce(
        state,
        Input.ScheduleTick,
        env.copy(now = at(local, kolkata), zone = kolkata, modes = listOf(sleep)),
    )

    @Test
    fun the_engine_starts_and_ends_scheduled_sessions_and_always_arms_the_next_wake() {
        val opened = tick(EngineState(), "2026-11-23T22:00")
        val session = (opened.outcome as Outcome.Started).session
        assertThat(session.startedBy).isEqualTo(SessionSource.Schedule)
        assertThat((opened.commands.last() as Command.ScheduleWake).at).isEqualTo(at("2026-11-24T07:00", kolkata))

        val closed = tick(opened.state, "2026-11-24T07:00")
        assertThat((closed.outcome as Outcome.Ended).session.endedBy).isEqualTo(SessionEnd.TriggerEnded)
        assertThat((closed.commands.last() as Command.ScheduleWake).at).isEqualTo(at("2026-11-24T22:00", kolkata))

        assertThat((tick(EngineState(), "2026-11-28T12:00").commands.single() as Command.ScheduleWake).at)
            .isEqualTo(at("2026-11-30T22:00", kolkata))
        val noSchedules = Engine.reduce(EngineState(), Input.ScheduleTick, env)
        assertThat((noSchedules.commands.single() as Command.ScheduleWake).at).isNull()
    }

    @Test
    fun an_emergency_unlock_keeps_the_schedule_from_restarting_until_its_next_window() {
        val opened = tick(EngineState(), "2026-11-23T22:00").state
        val unlocked = Engine.reduce(
            opened,
            Input.EmergencyUnlock,
            env.copy(now = at("2026-11-23T23:00", kolkata), modes = listOf(sleep)),
        )
        assertThat(unlocked.state.spentTriggers).containsExactly(TriggerKey.Schedule(sleep.id))

        val stillOpen = tick(unlocked.state, "2026-11-23T23:30")
        assertThat(stillOpen.outcome).isEqualTo(Outcome.TriggersChanged)
        assertThat(stillOpen.state.active).isNull()

        val closed = tick(stillOpen.state, "2026-11-24T07:00")
        assertThat(closed.state.spentTriggers).isEmpty()
        assertThat(tick(closed.state, "2026-11-24T22:00").outcome).isInstanceOf(Outcome.Started::class.java)
    }
}
