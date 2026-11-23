package io.github.uveshm003.anchor.core.engine

import io.github.uveshm003.anchor.core.engine.Command.ApplyAppPolicy
import io.github.uveshm003.anchor.core.engine.Command.CancelTimer
import io.github.uveshm003.anchor.core.engine.Command.ClearAppPolicy
import io.github.uveshm003.anchor.core.engine.Command.ScheduleTimer
import io.github.uveshm003.anchor.core.model.Anchor
import io.github.uveshm003.anchor.core.model.ExitRule
import io.github.uveshm003.anchor.core.model.Mode
import io.github.uveshm003.anchor.core.model.Session
import io.github.uveshm003.anchor.core.model.SessionEnd
import io.github.uveshm003.anchor.core.model.SessionId
import io.github.uveshm003.anchor.core.model.SessionSource
import io.github.uveshm003.anchor.core.model.StartTrigger
import io.github.uveshm003.anchor.core.model.UnlockRecord
import java.time.Duration

/**
 * The mode engine: a pure reducer (ADR 0003). One active session at a time; flexible to start, strict to stop;
 * essentials never blocked; emergency unlock always reachable while quota remains.
 */
object Engine {
    fun reduce(state: EngineState, input: Input, env: Environment): Transition = when (input) {
        is Input.HoldToStart -> holdToStart(state, input, env)
        Input.StopRequested -> stopRequested(state, env)
        is Input.AnchorTapped -> anchorTapped(state, input, env)
        Input.EmergencyUnlock -> emergencyUnlock(state, env)
        is Input.TimerElapsed -> timerElapsed(state, input, env)
        is Input.CommandFailed -> commandFailed(state, input)
        is Input.Reconcile -> reconcile(state, env)
    }

    private fun holdToStart(state: EngineState, input: Input.HoldToStart, env: Environment): Transition {
        val active = state.active
        val mode = env.modes.find { it.id == input.modeId }
        return when {
            active != null -> state.rejected(RejectReason.AlreadyActive(active.session.modeName))
            mode == null -> state.rejected(RejectReason.ModeNotFound)
            StartTrigger.Hold !in mode.startTriggers -> state.rejected(RejectReason.TriggerNotEnabled)
            else -> start(state, mode, SessionSource.Hold, env)
        }
    }

    private fun stopRequested(state: EngineState, env: Environment): Transition {
        val active = state.active ?: return state.rejected(RejectReason.NotActive)
        val reason = when (val rule = active.session.exitRule) {
            is ExitRule.RequiresAnchor -> RejectReason.RequiresAnchor(requiredAnchorName(rule, env))
            is ExitRule.TimerEnds -> RejectReason.EndsWithTimer(checkNotNull(active.session.endsAt))
            ExitRule.TriggerEnds -> RejectReason.EndsWithTrigger
        }
        return state.rejected(reason)
    }

    private fun anchorTapped(state: EngineState, input: Input.AnchorTapped, env: Environment): Transition {
        val anchor = env.anchors.find { it.hash == input.hash }
        val active = state.active
        return when {
            anchor == null -> state.ignored(IgnoreReason.UnknownAnchor)
            active == null -> startFromAnchor(state, anchor, env)
            else -> endWithAnchor(state, active, anchor, env)
        }
    }

    /** Idle: start the anchor's own mode, or the default mode. */
    private fun startFromAnchor(state: EngineState, anchor: Anchor, env: Environment): Transition {
        val modeId = anchor.modeId ?: env.defaultModeId
        val mode = modeId?.let { id -> env.modes.find { it.id == id } }
        return when {
            modeId == null -> state.rejected(RejectReason.NoDefaultMode)
            mode == null -> state.rejected(RejectReason.ModeNotFound)
            StartTrigger.AnchorTap !in mode.startTriggers -> state.rejected(RejectReason.TriggerNotEnabled)
            else -> start(state, mode, SessionSource.AnchorTap, env)
        }
    }

    private fun endWithAnchor(state: EngineState, active: ActiveSession, anchor: Anchor, env: Environment) =
        when (val rule = active.session.exitRule) {
            is ExitRule.RequiresAnchor ->
                if (rule.anchorId == null || rule.anchorId == anchor.id) {
                    end(state, active, SessionEnd.AnchorTap, env)
                } else {
                    state.rejected(RejectReason.RequiresAnchor(requiredAnchorName(rule, env)))
                }

            is ExitRule.TimerEnds -> state.rejected(RejectReason.EndsWithTimer(checkNotNull(active.session.endsAt)))

            ExitRule.TriggerEnds -> state.rejected(RejectReason.EndsWithTrigger)
        }

    private fun emergencyUnlock(state: EngineState, env: Environment): Transition {
        val active = state.active
        val remaining = EmergencyQuota.remaining(state.unlocks, env.now, env.emergencyQuotaPerWeek)
        return when {
            active == null -> state.rejected(RejectReason.NotActive)

            remaining == 0 ->
                state.rejected(RejectReason.QuotaExhausted(EmergencyQuota.nextAvailableAt(state.unlocks, env.now)))

            else -> {
                val unlocks = EmergencyQuota.counted(state.unlocks, env.now) + UnlockRecord(env.now, active.session.id)
                end(state.copy(unlocks = unlocks), active, SessionEnd.EmergencyUnlock, env)
            }
        }
    }

    private fun timerElapsed(state: EngineState, input: Input.TimerElapsed, env: Environment): Transition {
        val active = state.active
        if (active == null || active.session.id != input.sessionId) return state.ignored(IgnoreReason.StaleTimer)
        return end(state, active, SessionEnd.Timer, env)
    }

    private fun commandFailed(state: EngineState, input: Input.CommandFailed): Transition {
        val active = state.active
        if (active == null || active.session.id != input.sessionId) return state.ignored(IgnoreReason.StaleCommand)
        return Transition(state.copy(degraded = true), emptyList(), Outcome.Degraded(input.commandId))
    }

    /** The complete desired state for any state (ADR 0003 §4). Expired sessions end instead of re-applying. */
    private fun reconcile(state: EngineState, env: Environment): Transition {
        val active = state.active
        val endsAt = active?.session?.endsAt
        return when {
            active == null -> Transition(
                state.copy(degraded = false),
                listOf(ClearAppPolicy(commandId("clear", null, env))),
                Outcome.Reconciled,
            )

            endsAt != null && !env.now.isBefore(endsAt) -> end(state, active, SessionEnd.Timer, env)

            else -> Transition(state.copy(degraded = false), applyCommands(active, env), Outcome.Reconciled)
        }
    }

    private fun start(state: EngineState, mode: Mode, source: SessionSource, env: Environment): Transition {
        val endsAt = (mode.exitRule as? ExitRule.TimerEnds)?.let {
            env.now.plus(Duration.ofMinutes(it.minutes.toLong()))
        }
        val session = Session(
            id = SessionId(state.nextSessionId),
            modeId = mode.id,
            modeName = mode.name,
            startedAt = env.now,
            startedBy = source,
            exitRule = mode.exitRule,
            endsAt = endsAt,
        )
        val active = ActiveSession(session, mode)
        val next = state.copy(active = active, nextSessionId = state.nextSessionId + 1, degraded = false)
        return Transition(next, applyCommands(active, env), Outcome.Started(session))
    }

    private fun end(state: EngineState, active: ActiveSession, how: SessionEnd, env: Environment): Transition {
        val ended = active.session.copy(endedAt = env.now, endedBy = how)
        val commands = buildList {
            add(ClearAppPolicy(commandId("clear", ended.id, env)))
            if (ended.endsAt != null) add(CancelTimer(commandId("cancel_timer", ended.id, env), ended.id))
        }
        return Transition(state.copy(active = null, degraded = false), commands, Outcome.Ended(ended))
    }

    private fun applyCommands(active: ActiveSession, env: Environment): List<Command> {
        val session = active.session
        val rule = session.exitRule
        return buildList {
            add(
                ApplyAppPolicy(
                    id = commandId("apply", session.id, env),
                    sessionId = session.id,
                    modeName = session.modeName,
                    policy = EssentialsPolicy.apply(active.mode.appPolicy, env.essentials),
                    startedAt = session.startedAt,
                    exitRule = rule,
                    endsAt = session.endsAt,
                    requiredAnchorName = (rule as? ExitRule.RequiresAnchor)?.let { requiredAnchorName(it, env) },
                ),
            )
            session.endsAt?.let { add(ScheduleTimer(commandId("timer", session.id, env), session.id, it)) }
        }
    }

    private fun requiredAnchorName(rule: ExitRule.RequiresAnchor, env: Environment): String? =
        rule.anchorId?.let { id -> env.anchors.find { it.id == id }?.name }

    /** Unique per transition (the time is part of it) and stable once persisted in the outbox. */
    private fun commandId(kind: String, sessionId: SessionId?, env: Environment): String =
        "$kind:${sessionId?.value ?: 0}:${env.now.toEpochMilli()}"

    private fun EngineState.rejected(reason: RejectReason) = Transition(this, emptyList(), Outcome.Rejected(reason))

    private fun EngineState.ignored(reason: IgnoreReason) = Transition(this, emptyList(), Outcome.Ignored(reason))
}
