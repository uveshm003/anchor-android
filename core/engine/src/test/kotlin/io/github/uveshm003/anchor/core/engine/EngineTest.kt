package io.github.uveshm003.anchor.core.engine

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import io.github.uveshm003.anchor.core.engine.Command.ApplyAppPolicy
import io.github.uveshm003.anchor.core.engine.Command.CancelTimer
import io.github.uveshm003.anchor.core.engine.Command.ClearAppPolicy
import io.github.uveshm003.anchor.core.engine.Command.ScheduleTimer
import io.github.uveshm003.anchor.core.engine.Fixtures.DIALER
import io.github.uveshm003.anchor.core.engine.Fixtures.LAUNCHER
import io.github.uveshm003.anchor.core.engine.Fixtures.SOCIAL
import io.github.uveshm003.anchor.core.engine.Fixtures.activeIn
import io.github.uveshm003.anchor.core.engine.Fixtures.allow
import io.github.uveshm003.anchor.core.engine.Fixtures.anchorOnly
import io.github.uveshm003.anchor.core.engine.Fixtures.bedside
import io.github.uveshm003.anchor.core.engine.Fixtures.desk
import io.github.uveshm003.anchor.core.engine.Fixtures.env
import io.github.uveshm003.anchor.core.engine.Fixtures.holdOnly
import io.github.uveshm003.anchor.core.engine.Fixtures.now
import io.github.uveshm003.anchor.core.engine.Fixtures.specific
import io.github.uveshm003.anchor.core.engine.Fixtures.timer
import io.github.uveshm003.anchor.core.engine.Fixtures.trigger
import io.github.uveshm003.anchor.core.engine.Fixtures.work
import io.github.uveshm003.anchor.core.model.AppPolicy
import io.github.uveshm003.anchor.core.model.SessionEnd
import io.github.uveshm003.anchor.core.model.SessionId
import io.github.uveshm003.anchor.core.model.SessionSource
import io.github.uveshm003.anchor.core.model.UnlockRecord
import java.time.Duration
import kotlin.reflect.KClass
import kotlinx.serialization.json.Json
import org.junit.Test

class EngineTest {
    private val idle = EngineState()
    private val unknownHash = "c".repeat(64)

    private data class Case(
        val state: String,
        val input: Input,
        val outcome: KClass<out Outcome>,
        val commands: List<KClass<out Command>>,
    )

    private val states = mapOf(
        "idle" to idle,
        "any anchor" to activeIn(work),
        "timer" to activeIn(timer),
        "specific anchor" to activeIn(specific),
        "trigger" to activeIn(trigger),
    )

    private val started = Outcome.Started::class
    private val ended = Outcome.Ended::class
    private val rejected = Outcome.Rejected::class
    private val ignored = Outcome.Ignored::class
    private val reconciled = Outcome.Reconciled::class
    private val apply = ApplyAppPolicy::class
    private val clear = ClearAppPolicy::class
    private val schedule = ScheduleTimer::class
    private val cancel = CancelTimer::class
    private val scheduleRecovery = Command.ScheduleRecovery::class

    /** Every (state, input) pair: the outcome type and the command types, in order. */
    private val table = listOf(
        Case("idle", Input.HoldToStart(work.id), started, listOf(apply)),
        Case("idle", Input.StopRequested, rejected, emptyList()),
        Case("idle", Input.AnchorTapped(desk.hash), started, listOf(apply)),
        Case("idle", Input.EmergencyUnlock, rejected, emptyList()),
        Case("idle", Input.TimerElapsed(SessionId(1)), ignored, emptyList()),
        Case("idle", Input.CommandFailed("x", SessionId(1)), ignored, emptyList()),
        Case("idle", Input.Reconcile(ReconcileCause.AppStart), reconciled, listOf(clear)),
        Case("any anchor", Input.HoldToStart(work.id), rejected, emptyList()),
        Case("any anchor", Input.StopRequested, rejected, emptyList()),
        Case("any anchor", Input.AnchorTapped(desk.hash), ended, listOf(clear)),
        Case("any anchor", Input.EmergencyUnlock, ended, listOf(clear)),
        Case("any anchor", Input.TimerElapsed(SessionId(1)), ended, listOf(clear)),
        Case("any anchor", Input.CommandFailed("x", SessionId(1)), Outcome.Degraded::class, emptyList()),
        Case("any anchor", Input.Reconcile(ReconcileCause.Boot), reconciled, listOf(apply)),
        Case("timer", Input.HoldToStart(work.id), rejected, emptyList()),
        Case("timer", Input.StopRequested, rejected, emptyList()),
        Case("timer", Input.AnchorTapped(desk.hash), rejected, emptyList()),
        Case("timer", Input.EmergencyUnlock, ended, listOf(clear, cancel)),
        Case("timer", Input.TimerElapsed(SessionId(1)), ended, listOf(clear, cancel)),
        Case("timer", Input.CommandFailed("x", SessionId(2)), ignored, emptyList()),
        Case("timer", Input.Reconcile(ReconcileCause.TimeChanged), reconciled, listOf(apply, schedule)),
        Case("specific anchor", Input.HoldToStart(work.id), rejected, emptyList()),
        Case("specific anchor", Input.StopRequested, rejected, emptyList()),
        Case("specific anchor", Input.AnchorTapped(bedside.hash), rejected, emptyList()),
        Case("specific anchor", Input.EmergencyUnlock, ended, listOf(clear)),
        Case("specific anchor", Input.TimerElapsed(SessionId(2)), ignored, emptyList()),
        Case("specific anchor", Input.CommandFailed("x", SessionId(1)), Outcome.Degraded::class, emptyList()),
        Case("specific anchor", Input.Reconcile(ReconcileCause.PackageReplaced), reconciled, listOf(apply)),
        Case("trigger", Input.HoldToStart(work.id), rejected, emptyList()),
        Case("trigger", Input.StopRequested, rejected, emptyList()),
        Case("trigger", Input.AnchorTapped(desk.hash), rejected, emptyList()),
        Case("trigger", Input.EmergencyUnlock, ended, listOf(clear)),
        Case("trigger", Input.TimerElapsed(SessionId(1)), ended, listOf(clear)),
        Case("trigger", Input.CommandFailed("x", null), ignored, emptyList()),
        Case("trigger", Input.Reconcile(ReconcileCause.TimeZoneChanged), reconciled, listOf(apply)),
        Case("idle", Input.RecoveryRequested, rejected, emptyList()),
        Case("idle", Input.RecoveryCancelled, ignored, emptyList()),
        Case("idle", Input.RecoveryElapsed(SessionId(1)), ignored, emptyList()),
        Case("any anchor", Input.RecoveryRequested, Outcome.RecoveryScheduled::class, listOf(scheduleRecovery)),
        Case("any anchor", Input.RecoveryCancelled, ignored, emptyList()),
        Case("any anchor", Input.RecoveryElapsed(SessionId(1)), ignored, emptyList()),
        Case("timer", Input.RecoveryRequested, Outcome.RecoveryScheduled::class, listOf(scheduleRecovery)),
        Case("timer", Input.RecoveryCancelled, ignored, emptyList()),
        Case("timer", Input.RecoveryElapsed(SessionId(1)), ignored, emptyList()),
        Case("specific anchor", Input.RecoveryRequested, Outcome.RecoveryScheduled::class, listOf(scheduleRecovery)),
        Case("specific anchor", Input.RecoveryCancelled, ignored, emptyList()),
        Case("specific anchor", Input.RecoveryElapsed(SessionId(1)), ignored, emptyList()),
        Case("trigger", Input.RecoveryRequested, Outcome.RecoveryScheduled::class, listOf(scheduleRecovery)),
        Case("trigger", Input.RecoveryCancelled, ignored, emptyList()),
        Case("trigger", Input.RecoveryElapsed(SessionId(1)), ignored, emptyList()),
    )

    @Test
    fun `every state and input pair`() {
        // Every input kind appears for every state (kind() is an exhaustive when, so new inputs break the build).
        states.keys.forEach { name ->
            assertWithMessage(name).that(
                table.filter {
                    it.state == name
                }.map { kind(it.input) }.toSet(),
            ).isEqualTo(ALL_KINDS)
        }

        table.forEach { case ->
            val transition = Engine.reduce(states.getValue(case.state), case.input, env)
            val label = "${case.state} + ${case.input}"
            assertWithMessage(label).that(transition.outcome::class).isEqualTo(case.outcome)
            assertWithMessage(label).that(transition.commands.map { it::class }).isEqualTo(case.commands)
            if (case.outcome == rejected || case.outcome == ignored) {
                assertWithMessage(label).that(transition.state).isEqualTo(states.getValue(case.state))
            }
        }
    }

    private fun kind(input: Input): String = when (input) {
        is Input.HoldToStart -> "hold"
        Input.StopRequested -> "stop"
        is Input.AnchorTapped -> "anchor"
        Input.EmergencyUnlock -> "emergency"
        is Input.TimerElapsed -> "timer"
        is Input.CommandFailed -> "failed"
        is Input.Reconcile -> "reconcile"
        Input.RecoveryRequested -> "recovery_requested"
        Input.RecoveryCancelled -> "recovery_cancelled"
        is Input.RecoveryElapsed -> "recovery_elapsed"
    }

    @Test
    fun `hold to start opens one session with essentials removed from the block list`() {
        val transition = Engine.reduce(idle, Input.HoldToStart(work.id), env)

        val session = (transition.outcome as Outcome.Started).session
        assertThat(session.id).isEqualTo(SessionId(1))
        assertThat(session.startedBy).isEqualTo(SessionSource.Hold)
        assertThat(session.startedAt).isEqualTo(now)
        assertThat(transition.state.active?.session).isEqualTo(session)
        assertThat(transition.state.nextSessionId).isEqualTo(2)
        val apply = transition.commands.single() as ApplyAppPolicy
        assertThat(apply.policy).isEqualTo(AppPolicy.BlockList(setOf(SOCIAL)))
        assertThat(apply.modeName).isEqualTo("Work")
        assertThat(apply.requiredAnchorName).isNull()
    }

    @Test
    fun `starting while active is rejected with the running mode and no commands`() {
        val transition = Engine.reduce(activeIn(timer), Input.HoldToStart(work.id), env)

        assertThat(transition.outcome).isEqualTo(Outcome.Rejected(RejectReason.AlreadyActive("Nap")))
        assertThat(transition.commands).isEmpty()
    }

    @Test
    fun `hold to start rejects unknown modes and modes without the hold trigger`() {
        assertThat(
            Engine.reduce(idle, Input.HoldToStart(io.github.uveshm003.anchor.core.model.ModeId(99)), env).outcome,
        )
            .isEqualTo(Outcome.Rejected(RejectReason.ModeNotFound))
        assertThat(Engine.reduce(idle, Input.HoldToStart(anchorOnly.id), env).outcome)
            .isEqualTo(Outcome.Rejected(RejectReason.TriggerNotEnabled))
    }

    @Test
    fun `allow list always includes essentials`() {
        val apply = Engine.reduce(idle, Input.HoldToStart(allow.id), env).commands.single() as ApplyAppPolicy

        assertThat(apply.policy).isEqualTo(AppPolicy.AllowList(setOf("com.maps", DIALER, LAUNCHER)))
        assertThat(apply.policy.blocks(DIALER)).isFalse()
        assertThat(apply.policy.blocks(SOCIAL)).isTrue()
    }

    @Test
    fun `timer modes schedule their end and stop explains when they end`() {
        val transition = Engine.reduce(idle, Input.HoldToStart(timer.id), env)
        val endsAt = now.plus(Duration.ofMinutes(30))

        assertThat(transition.commands.filterIsInstance<ScheduleTimer>().single().at).isEqualTo(endsAt)
        assertThat(Engine.reduce(transition.state, Input.StopRequested, env).outcome)
            .isEqualTo(Outcome.Rejected(RejectReason.EndsWithTimer(endsAt)))
        assertThat(Engine.reduce(transition.state, Input.AnchorTapped(desk.hash), env).outcome)
            .isEqualTo(Outcome.Rejected(RejectReason.EndsWithTimer(endsAt)))
    }

    @Test
    fun `stale timer for another session is ignored`() {
        val transition = Engine.reduce(activeIn(timer), Input.TimerElapsed(SessionId(42)), env)

        assertThat(transition.outcome).isEqualTo(Outcome.Ignored(IgnoreReason.StaleTimer))
    }

    @Test
    fun `stop and other anchors name the anchor a specific-anchor session needs`() {
        val state = activeIn(specific)
        val expected = Outcome.Rejected(RejectReason.RequiresAnchor("Desk"))

        assertThat(Engine.reduce(state, Input.StopRequested, env).outcome).isEqualTo(expected)
        assertThat(Engine.reduce(state, Input.AnchorTapped(bedside.hash), env).outcome).isEqualTo(expected)
        assertThat(Engine.reduce(activeIn(work), Input.StopRequested, env).outcome)
            .isEqualTo(Outcome.Rejected(RejectReason.RequiresAnchor(null)))
        assertThat(Engine.reduce(activeIn(trigger), Input.StopRequested, env).outcome)
            .isEqualTo(Outcome.Rejected(RejectReason.EndsWithTrigger))
        assertThat(
            (
                Engine.reduce(
                    idle,
                    Input.HoldToStart(specific.id),
                    env,
                ).commands.single() as ApplyAppPolicy
                ).requiredAnchorName,
        )
            .isEqualTo("Desk")
    }

    @Test
    fun `the matching specific anchor ends the session`() {
        val transition = Engine.reduce(activeIn(specific), Input.AnchorTapped(desk.hash), env)

        val ended = (transition.outcome as Outcome.Ended).session
        assertThat(ended.endedBy).isEqualTo(SessionEnd.AnchorTap)
        assertThat(ended.endedAt).isEqualTo(now)
        assertThat(transition.state.active).isNull()
    }

    @Test
    fun `anchor taps start the anchor's mode or the default mode and ignore unknown tags`() {
        val bound = Engine.reduce(idle, Input.AnchorTapped(bedside.hash), env).outcome as Outcome.Started
        assertThat(bound.session.modeId).isEqualTo(timer.id)
        assertThat(bound.session.startedBy).isEqualTo(SessionSource.AnchorTap)

        val default = Engine.reduce(idle, Input.AnchorTapped(desk.hash), env).outcome as Outcome.Started
        assertThat(default.session.modeId).isEqualTo(work.id)

        assertThat(Engine.reduce(idle, Input.AnchorTapped(unknownHash), env).outcome)
            .isEqualTo(Outcome.Ignored(IgnoreReason.UnknownAnchor))
        assertThat(Engine.reduce(activeIn(work), Input.AnchorTapped(unknownHash), env).outcome)
            .isEqualTo(Outcome.Ignored(IgnoreReason.UnknownAnchor))
    }

    @Test
    fun `anchor tap without a usable default mode is rejected`() {
        assertThat(Engine.reduce(idle, Input.AnchorTapped(desk.hash), env.copy(defaultModeId = null)).outcome)
            .isEqualTo(Outcome.Rejected(RejectReason.NoDefaultMode))
        assertThat(
            Engine.reduce(
                idle,
                Input.AnchorTapped(desk.hash),
                env.copy(defaultModeId = io.github.uveshm003.anchor.core.model.ModeId(99)),
            ).outcome,
        )
            .isEqualTo(Outcome.Rejected(RejectReason.ModeNotFound))
        assertThat(Engine.reduce(idle, Input.AnchorTapped(desk.hash), env.copy(defaultModeId = holdOnly.id)).outcome)
            .isEqualTo(Outcome.Rejected(RejectReason.TriggerNotEnabled))
    }

    @Test
    fun `emergency unlock ends the session and is counted`() {
        val transition = Engine.reduce(activeIn(work), Input.EmergencyUnlock, env)

        assertThat((transition.outcome as Outcome.Ended).session.endedBy).isEqualTo(SessionEnd.EmergencyUnlock)
        assertThat(transition.state.unlocks).containsExactly(UnlockRecord(now, SessionId(1)))
    }

    @Test
    fun `quota is a rolling seven days including the edges and a clock moved back`() {
        val exactlyWeekAgo = UnlockRecord(now.minus(EmergencyQuota.WINDOW), SessionId(1))
        val justInside = UnlockRecord(now.minus(EmergencyQuota.WINDOW).plusSeconds(1), SessionId(2))
        val future = UnlockRecord(now.plusSeconds(3600), SessionId(3))
        val recent = UnlockRecord(now.minusSeconds(60), SessionId(4))

        // Exactly 7 days old no longer counts.
        val withRoom =
            activeIn(work, EngineState(unlocks = listOf(exactlyWeekAgo, justInside, recent), nextSessionId = 5))
        val unlocked = Engine.reduce(withRoom, Input.EmergencyUnlock, env)
        assertThat(unlocked.outcome).isInstanceOf(Outcome.Ended::class.java)
        assertThat(
            unlocked.state.unlocks.map {
                it.sessionId
            },
        ).containsExactly(SessionId(2), SessionId(4), SessionId(5))

        // An unlock dated in the future (clock moved back) still counts.
        val full = activeIn(work, EngineState(unlocks = listOf(justInside, recent, future), nextSessionId = 5))
        assertThat(Engine.reduce(full, Input.EmergencyUnlock, env).outcome)
            .isEqualTo(Outcome.Rejected(RejectReason.QuotaExhausted(justInside.at.plus(EmergencyQuota.WINDOW))))

        // A smaller quota from settings applies.
        val one = activeIn(work, EngineState(unlocks = listOf(recent), nextSessionId = 5))
        assertThat(Engine.reduce(one, Input.EmergencyUnlock, env.copy(emergencyQuotaPerWeek = 1)).outcome)
            .isInstanceOf(Outcome.Rejected::class.java)
        assertThat(EmergencyQuota.nextAvailableAt(emptyList(), now)).isEqualTo(now)
    }

    @Test
    fun `reconcile ends an expired timer session instead of re-applying it`() {
        val later = env.copy(now = now.plus(Duration.ofMinutes(31)))
        val transition = Engine.reduce(activeIn(timer), Input.Reconcile(ReconcileCause.Boot), later)

        assertThat((transition.outcome as Outcome.Ended).session.endedBy).isEqualTo(SessionEnd.Timer)
        assertThat(transition.commands.map { it::class }).containsExactly(clear, cancel).inOrder()
    }

    @Test
    fun `a failure marks the session degraded and reconcile clears it`() {
        val degraded = Engine.reduce(activeIn(work), Input.CommandFailed("apply:1:0", SessionId(1)), env).state
        assertThat(degraded.degraded).isTrue()

        assertThat(Engine.reduce(degraded, Input.Reconcile(ReconcileCause.AppStart), env).state.degraded).isFalse()
    }

    @Test
    fun `command ids are unique per transition and state survives JSON`() {
        val first = Engine.reduce(idle, Input.HoldToStart(work.id), env)
        val again = Engine.reduce(
            first.state,
            Input.Reconcile(ReconcileCause.AppStart),
            env.copy(now = now.plusSeconds(1)),
        )

        assertThat(first.commands.single().id).isNotEqualTo(again.commands.single().id)
        assertThat(Json.decodeFromString<EngineState>(Json.encodeToString(first.state))).isEqualTo(first.state)
        val commands: List<Command> = first.commands + again.commands
        assertThat(Json.decodeFromString<List<Command>>(Json.encodeToString(commands))).isEqualTo(commands)
    }

    private companion object {
        val ALL_KINDS = setOf(
            "hold",
            "stop",
            "anchor",
            "emergency",
            "timer",
            "failed",
            "reconcile",
            "recovery_requested",
            "recovery_cancelled",
            "recovery_elapsed",
        )
    }
}
