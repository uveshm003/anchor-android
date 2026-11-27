package io.github.uveshm003.anchor.core.data.session

import io.github.uveshm003.anchor.core.common.dispatchers.ApplicationScope
import io.github.uveshm003.anchor.core.engine.Command
import io.github.uveshm003.anchor.core.engine.CommandExecutor
import io.github.uveshm003.anchor.core.engine.Engine
import io.github.uveshm003.anchor.core.engine.ExecutionResult
import io.github.uveshm003.anchor.core.engine.Input
import io.github.uveshm003.anchor.core.engine.Outcome
import io.github.uveshm003.anchor.core.engine.ReconcileCause
import io.github.uveshm003.anchor.core.engine.SessionInputs
import io.github.uveshm003.anchor.core.model.SessionId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

/**
 * Runs the engine (ADR 0003 §2–3): [submit] only enqueues; one consumer takes each input, samples the
 * [Environment] once, reduces, persists the new state **and** its commands (outbox) in one transaction, then
 * executes the pending commands in order. A failed command is reduced as [Input.CommandFailed] before the next
 * input. Pending commands left by a crash run again on [start].
 */
@Singleton
class SessionController @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    private val environment: EnvironmentSampler,
    private val store: EngineStore,
    executors: Set<@JvmSuppressWildcards CommandExecutor>,
) : SessionInputs,
    SessionRequests {
    private val registry: Map<KClass<out Command>, CommandExecutor> = buildRegistry(executors)
    private val queue = Channel<Queued>(Channel.UNLIMITED)
    private val _outcomes = MutableSharedFlow<Outcome>(extraBufferCapacity = OUTCOME_BUFFER)

    /** Every outcome, for UI that reacts to taps it didn't submit (e.g. "Tap your Desk anchor"). */
    val outcomes: SharedFlow<Outcome> = _outcomes

    private class Queued(val input: Input, val result: CompletableDeferred<Outcome>?)

    /**
     * Starts the consumer: re-runs pending commands, then reconciles (app start), and reconciles again whenever
     * the saved modes change (a new schedule must be armed right away). Call once.
     */
    fun start() {
        // Queued synchronously, so it runs before anything submitted after start().
        queue.trySend(Queued(Input.Reconcile(ReconcileCause.AppStart), null))
        scope.launch {
            executePending()
            for (queued in queue) {
                val outcome = process(queued.input)
                queued.result?.complete(outcome)
            }
        }
        scope.launch {
            environment.modeChanges.collect { queue.send(Queued(Input.Reconcile(ReconcileCause.ModesChanged), null)) }
        }
    }

    override suspend fun submit(input: Input) {
        queue.send(Queued(input, null))
    }

    /** Submits [input] and waits for its outcome (for the UI's own actions). */
    override suspend fun request(input: Input): Outcome {
        val result = CompletableDeferred<Outcome>()
        queue.send(Queued(input, result))
        return result.await()
    }

    private suspend fun process(input: Input): Outcome {
        val transition = Engine.reduce(store.load(), input, environment.sample())
        store.persist(transition)
        _outcomes.tryEmit(transition.outcome)
        executePending()
        return transition.outcome
    }

    private suspend fun executePending() {
        for ((rowId, command) in store.pending()) {
            when (registry.getValue(command::class).execute(command)) {
                ExecutionResult.Done -> store.markDone(rowId)

                is ExecutionResult.Failed -> {
                    store.markFailed(rowId)
                    // Reduced now, before the next queued input (no re-entry into submit).
                    process(Input.CommandFailed(command.id, command.sessionId()))
                }
            }
        }
    }

    private fun Command.sessionId(): SessionId? = when (this) {
        is Command.ApplyAppPolicy -> sessionId
        is Command.ScheduleTimer -> sessionId
        is Command.CancelTimer -> sessionId
        is Command.ScheduleRecovery -> sessionId
        is Command.CancelRecovery -> sessionId
        is Command.ShowSessionNotification -> sessionId
        is Command.ClearAppPolicy, is Command.HideSessionNotification, is Command.ScheduleWake -> null
    }

    companion object {
        private const val OUTCOME_BUFFER = 16

        /** Every command type the engine emits; each needs exactly one executor. */
        val COMMAND_TYPES: Set<KClass<out Command>> = setOf(
            Command.ApplyAppPolicy::class,
            Command.ClearAppPolicy::class,
            Command.ScheduleTimer::class,
            Command.CancelTimer::class,
            Command.ScheduleRecovery::class,
            Command.CancelRecovery::class,
            Command.ShowSessionNotification::class,
            Command.HideSessionNotification::class,
            Command.ScheduleWake::class,
        )

        /** Fails fast on a missing or duplicate handler, so "not mine" and "failed" are never confused. */
        fun buildRegistry(executors: Set<CommandExecutor>): Map<KClass<out Command>, CommandExecutor> {
            val pairs = executors.flatMap { executor -> executor.handles.map { it to executor } }
            val duplicates = pairs.groupBy({ it.first }, { it.second }).filterValues { it.size > 1 }.keys
            check(duplicates.isEmpty()) { "More than one executor for $duplicates" }
            val registry = pairs.toMap()
            val missing = COMMAND_TYPES - registry.keys
            check(missing.isEmpty()) { "No executor for $missing" }
            return registry
        }
    }
}
