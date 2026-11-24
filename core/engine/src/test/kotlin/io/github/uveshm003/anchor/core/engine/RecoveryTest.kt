package io.github.uveshm003.anchor.core.engine

import com.google.common.truth.Truth.assertThat
import io.github.uveshm003.anchor.core.engine.Fixtures.activeIn
import io.github.uveshm003.anchor.core.engine.Fixtures.desk
import io.github.uveshm003.anchor.core.engine.Fixtures.env
import io.github.uveshm003.anchor.core.engine.Fixtures.now
import io.github.uveshm003.anchor.core.engine.Fixtures.timer
import io.github.uveshm003.anchor.core.engine.Fixtures.work
import io.github.uveshm003.anchor.core.model.SessionEnd
import io.github.uveshm003.anchor.core.model.SessionId
import io.github.uveshm003.anchor.core.model.UnlockRecord
import java.time.Duration
import org.junit.Test

class RecoveryTest {
    private val hour = Duration.ofHours(1).toMillis()
    private val boot1 = env.copy(elapsedRealtimeMillis = 1_000, bootCount = 1)

    /** Environment [hours] of real time later in boot [boot] with elapsedRealtime [elapsed]. */
    private fun at(hours: Long, elapsed: Long, boot: Int = 1) =
        env.copy(now = now.plus(Duration.ofHours(hours)), elapsedRealtimeMillis = elapsed, bootCount = boot)

    private fun requested(state: EngineState = activeIn(work)) = Engine.reduce(state, Input.RecoveryRequested, boot1)

    @Test
    fun request_schedules_a_monotonic_check_24h_out_whatever_the_quota_or_exit_rule() {
        val noQuota = activeIn(timer, EngineState(unlocks = List(3) { UnlockRecord(now, SessionId(0)) }))
        val transition = requested(noQuota)

        assertThat(transition.outcome).isEqualTo(Outcome.RecoveryScheduled(Recovery.DELAY))
        val schedule = transition.commands.single() as Command.ScheduleRecovery
        assertThat(schedule.elapsedRealtimeAt).isEqualTo(1_000 + 24 * hour)
        assertThat(Engine.reduce(transition.state, Input.RecoveryRequested, boot1).outcome)
            .isEqualTo(Outcome.Rejected(RejectReason.RecoveryAlreadyPending))
    }

    @Test
    fun elapsed_after_24h_of_monotonic_time_ends_the_session() {
        val state = requested().state
        val transition = Engine.reduce(state, Input.RecoveryElapsed(SessionId(1)), at(24, 1_000 + 24 * hour))

        assertThat((transition.outcome as Outcome.Ended).session.endedBy).isEqualTo(SessionEnd.Recovery)
        assertThat(transition.state.recovery).isNull()
        assertThat(transition.state.active).isNull()
    }

    @Test
    fun moving_the_wall_clock_forward_does_not_complete_it() {
        val state = requested().state
        // Wall clock jumped 30 h ahead, but only 1 h of monotonic time passed.
        val transition = Engine.reduce(state, Input.RecoveryElapsed(SessionId(1)), at(30, 1_000 + hour))

        assertThat(transition.outcome).isEqualTo(Outcome.RecoveryScheduled(Duration.ofHours(23)))
        assertThat(transition.state.active).isNotNull()
    }

    @Test
    fun a_reboot_pauses_but_never_resets_and_the_wall_clock_limit_completes_it() {
        var state = requested().state
        // 10 h counted in boot 1 (stamped by a reconcile), then a reboot.
        state = Engine.reduce(state, Input.Reconcile(ReconcileCause.AppStart), at(10, 1_000 + 10 * hour)).state
        val afterReboot = Engine.reduce(state, Input.Reconcile(ReconcileCause.Boot), at(12, 5_000, boot = 2))
        assertThat(afterReboot.state.recovery?.countedMillis).isEqualTo(10 * hour)
        assertThat(afterReboot.commands.filterIsInstance<Command.ScheduleRecovery>().single().elapsedRealtimeAt)
            .isEqualTo(5_000 + 14 * hour)

        // 14 more hours in boot 2 complete it.
        val done = Engine.reduce(
            afterReboot.state,
            Input.RecoveryElapsed(SessionId(1)),
            at(26, 5_000 + 14 * hour, boot = 2),
        )
        assertThat((done.outcome as Outcome.Ended).session.endedBy).isEqualTo(SessionEnd.Recovery)

        // Even with no monotonic time counted, 48 h of wall clock completes it.
        val lost = Engine.reduce(afterReboot.state, Input.Reconcile(ReconcileCause.Boot), at(48, 0, boot = 3))
        assertThat((lost.outcome as Outcome.Ended).session.endedBy).isEqualTo(SessionEnd.Recovery)
    }

    @Test
    fun the_next_check_is_never_later_than_the_wall_clock_limit() {
        val state = requested().state
        // 47 h of wall clock but only 1 h counted (lost boots): check again within the hour left.
        val transition = Engine.reduce(state, Input.RecoveryElapsed(SessionId(1)), at(47, 1_000 + hour))

        assertThat(
            (transition.commands.single() as Command.ScheduleRecovery).elapsedRealtimeAt,
        ).isEqualTo(1_000 + hour + hour)
    }

    @Test
    fun cancelling_or_ending_another_way_cancels_the_alarm() {
        val state = requested().state

        val cancelled = Engine.reduce(state, Input.RecoveryCancelled, boot1)
        assertThat(cancelled.outcome).isEqualTo(Outcome.RecoveryCancelled)
        assertThat(cancelled.commands.single()).isInstanceOf(Command.CancelRecovery::class.java)
        assertThat(cancelled.state.recovery).isNull()

        val tapped = Engine.reduce(state, Input.AnchorTapped(desk.hash), boot1)
        assertThat(tapped.commands.map { it::class }).contains(Command.CancelRecovery::class)
        assertThat(tapped.state.recovery).isNull()

        assertThat(Engine.reduce(state, Input.RecoveryElapsed(SessionId(9)), boot1).outcome)
            .isEqualTo(Outcome.Ignored(IgnoreReason.NoRecovery))
    }
}
