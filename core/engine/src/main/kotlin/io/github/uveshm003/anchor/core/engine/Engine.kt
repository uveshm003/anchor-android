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
        is Input.Reconcile -> reconcile(state, env).let { syncSchedules(it, env) }
        Input.RecoveryRequested, Input.RecoveryCancelled, is Input.RecoveryElapsed -> recovery(state, input, env)
        is Input.TriggerStarted, is Input.TriggerEnded, Input.ScheduleTick -> automatic(state, input, env)
    }

    private fun recovery(state: EngineState, input: Input, env: Environment): Transition = when (input) {
        Input.RecoveryRequested -> recoveryRequested(state, env)
        Input.RecoveryCancelled -> recoveryCancelled(state, env)
        is Input.RecoveryElapsed -> recoveryElapsed(state, input, env)
        else -> error("Not a recovery input: $input")
    }

    private fun automatic(state: EngineState, input: Input, env: Environment): Transition = when (input) {
        is Input.TriggerStarted -> triggerStarted(state, input.key, env)
        is Input.TriggerEnded -> triggerEnded(state, input.key, env)
        Input.ScheduleTick -> syncSchedules(Transition(state, emptyList(), Outcome.TriggersChanged), env)
        else -> error("Not an automatic-trigger input: $input")
    }

    /**
     * Brings schedule triggers in line with the clock: closed windows end their trigger (and un-spend it), newly open
     * ones start, spent ones stay quiet. Always re-arms the wake-up for the next opening or closing.
     */
    private fun syncSchedules(after: Transition, env: Environment): Transition {
        val open = Schedules.openKeys(env.modes, env.now, env.zone)
        val tracked = (after.state.activeTriggers + after.state.spentTriggers)
            .filterIsInstance<TriggerKey.Schedule>()
            .toSet()
        var result = after
        (tracked - open).forEach { result = result.then(triggerEnded(result.state, it, env)) }
        (open - tracked).forEach { result = result.then(triggerStarted(result.state, it, env)) }
        val wake = Command.ScheduleWake(
            id = commandId("schedule_wake", null, env),
            at = Schedules.nextWake(env.modes, env.now, env.zone),
        )
        return result.copy(commands = result.commands + wake)
    }

    /** Chains two transitions; an end followed by a start is recorded as a switch so both sessions are kept. */
    private fun Transition.then(next: Transition): Transition {
        val first = outcome
        val second = next.outcome
        val combined = when {
            first is Outcome.Ended && second is Outcome.Started -> Outcome.Switched(first.session, second.session)
            second == Outcome.TriggersChanged -> first
            else -> second
        }
        return Transition(next.state, commands + next.commands, combined)
    }

    /**
     * Automatic triggers never override a manual or anchor session. Idle: the highest-priority matching mode
     * starts. A higher-priority trigger replaces a running automatic session only if that session ends with its
     * trigger (otherwise ending it would break "strict to stop").
     */
    private fun triggerStarted(state: EngineState, key: TriggerKey, env: Environment): Transition {
        // Used up while still true: wait until it turns false before it can start anything again.
        if (key in state.spentTriggers) return Transition(state, emptyList(), Outcome.TriggersChanged)
        val noted = state.copy(activeTriggers = state.activeTriggers + key)
        val active = state.active
        val best = Triggers.best(noted.activeTriggers, env.modes)
        val preempts = active?.trigger != null && active.session.exitRule == ExitRule.TriggerEnds &&
            best != null && best.mode.id != active.mode.id && best.mode.priority < active.mode.priority
        return when {
            best == null -> Transition(noted, emptyList(), Outcome.TriggersChanged)
            active == null -> start(noted, best.mode, Triggers.source(best.key), env, best.key)
            preempts -> end(noted, active, SessionEnd.Preempted, env)
            else -> Transition(noted, emptyList(), Outcome.TriggersChanged)
        }
    }

    /** Ends only the session this trigger started, and only if its exit rule is "ends with its trigger". */
    private fun triggerEnded(state: EngineState, key: TriggerKey, env: Environment): Transition {
        val noted = state.copy(activeTriggers = state.activeTriggers - key, spentTriggers = state.spentTriggers - key)
        val active = state.active
        return if (active != null && active.trigger == key && active.session.exitRule == ExitRule.TriggerEnds) {
            end(noted, active, SessionEnd.TriggerEnded, env)
        } else {
            Transition(noted, emptyList(), Outcome.TriggersChanged)
        }
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
                // A specific anchor that's no longer paired falls back to any anchor, so nobody is stuck.
                if (rule.anchorId == null || rule.anchorId == anchor.id ||
                    env.anchors.none { it.id == rule.anchorId }
                ) {
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
        val recovery = state.recovery?.let { Recovery.restamp(it, env) }
        return when {
            active == null -> Transition(
                state.copy(degraded = false, recovery = null),
                listOf(
                    ClearAppPolicy(commandId("clear", null, env)),
                    Command.HideSessionNotification(commandId("hide_notification", null, env)),
                ),
                Outcome.Reconciled,
            )

            endsAt != null && !env.now.isBefore(endsAt) -> end(state, active, SessionEnd.Timer, env)

            recovery != null && Recovery.isComplete(recovery, env) -> end(state, active, SessionEnd.Recovery, env)

            else -> Transition(
                state.copy(degraded = false, recovery = recovery),
                applyCommands(active, env) + listOfNotNull(recovery?.let { scheduleRecovery(it, env) }),
                Outcome.Reconciled,
            )
        }
    }

    /** Works whatever the exit rule or the emergency quota; one pending recovery at a time. */
    private fun recoveryRequested(state: EngineState, env: Environment): Transition {
        val active = state.active
        return when {
            active == null -> state.rejected(RejectReason.NotActive)

            state.recovery != null -> state.rejected(RejectReason.RecoveryAlreadyPending)

            else -> {
                val recovery = Recovery.start(active.session.id, env)
                Transition(
                    state.copy(recovery = recovery),
                    listOf(scheduleRecovery(recovery, env)),
                    Outcome.RecoveryScheduled(Recovery.DELAY),
                )
            }
        }
    }

    private fun recoveryCancelled(state: EngineState, env: Environment): Transition {
        val recovery = state.recovery ?: return state.ignored(IgnoreReason.NoRecovery)
        val cancel = Command.CancelRecovery(commandId("cancel_recovery", recovery.sessionId, env), recovery.sessionId)
        return Transition(state.copy(recovery = null), listOf(cancel), Outcome.RecoveryCancelled)
    }

    /** The alarm fired: end the session if enough monotonic time has passed, otherwise wake up again later. */
    private fun recoveryElapsed(state: EngineState, input: Input.RecoveryElapsed, env: Environment): Transition {
        val active = state.active
        val recovery = state.recovery?.takeIf { it.sessionId == input.sessionId }?.let { Recovery.restamp(it, env) }
        return when {
            active == null || recovery == null -> state.ignored(IgnoreReason.NoRecovery)

            Recovery.isComplete(recovery, env) -> end(state, active, SessionEnd.Recovery, env)

            else -> Transition(
                state.copy(recovery = recovery),
                listOf(scheduleRecovery(recovery, env)),
                Outcome.RecoveryScheduled(Recovery.remaining(recovery)),
            )
        }
    }

    /** Next check: when the monotonic delay or the wall-clock limit is due, whichever comes first. */
    private fun scheduleRecovery(recovery: PendingRecovery, env: Environment): Command.ScheduleRecovery {
        val untilLimit = Duration.between(env.now, recovery.requestedAt.plus(Recovery.WALL_CLOCK_LIMIT))
        val wait = minOf(Recovery.remaining(recovery), untilLimit).coerceAtLeast(Duration.ZERO)
        return Command.ScheduleRecovery(
            id = commandId("recovery", recovery.sessionId, env),
            sessionId = recovery.sessionId,
            elapsedRealtimeAt = env.elapsedRealtimeMillis + wait.toMillis(),
        )
    }

    private fun start(
        state: EngineState,
        mode: Mode,
        source: SessionSource,
        env: Environment,
        trigger: TriggerKey? = null,
    ): Transition {
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
        val active = ActiveSession(session, mode, trigger)
        val next = state.copy(active = active, nextSessionId = state.nextSessionId + 1, degraded = false)
        return Transition(next, applyCommands(active, env), Outcome.Started(session))
    }

    private fun end(state: EngineState, active: ActiveSession, how: SessionEnd, env: Environment): Transition {
        val ended = active.session.copy(endedAt = env.now, endedBy = how)
        val commands = buildList {
            add(ClearAppPolicy(commandId("clear", ended.id, env)))
            add(Command.HideSessionNotification(commandId("hide_notification", ended.id, env)))
            if (ended.endsAt != null) add(CancelTimer(commandId("cancel_timer", ended.id, env), ended.id))
            if (state.recovery !=
                null
            ) {
                add(Command.CancelRecovery(commandId("cancel_recovery", ended.id, env), ended.id))
            }
        }
        val idle = state.copy(
            active = null,
            degraded = false,
            recovery = null,
            activeTriggers = remainingTriggers(state, active, how),
            spentTriggers = state.spentTriggers + (state.activeTriggers - remainingTriggers(state, active, how)),
        )
        // A trigger that is still active may start its mode now
        // (e.g. a schedule still open when a manual session ends).
        val next = Triggers.best(idle.activeTriggers, env.modes)
            ?: return Transition(idle, commands, Outcome.Ended(ended))
        val started = start(idle, next.mode, Triggers.source(next.key), env, next.key)
        return Transition(started.state, commands + started.commands, Outcome.Switched(ended, started.activeSession()))
    }

    /**
     * Triggers still eligible after a session ends. The trigger that started it is used up unless it was only
     * preempted; after an emergency unlock or recovery all are, so the session doesn't restart under the user.
     */
    private fun remainingTriggers(state: EngineState, active: ActiveSession, how: SessionEnd): Set<TriggerKey> =
        when (how) {
            SessionEnd.EmergencyUnlock, SessionEnd.Recovery -> emptySet()
            SessionEnd.Preempted -> state.activeTriggers
            else -> state.activeTriggers - setOfNotNull(active.trigger)
        }

    private fun Transition.activeSession() = checkNotNull(state.active).session

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
            add(
                Command.ShowSessionNotification(
                    id = commandId("show_notification", session.id, env),
                    sessionId = session.id,
                    modeName = session.modeName,
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
