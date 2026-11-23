package io.github.uveshm003.anchor.core.data.session

import io.github.uveshm003.anchor.core.data.platform.EssentialApps
import io.github.uveshm003.anchor.core.data.repository.AnchorRepository
import io.github.uveshm003.anchor.core.data.repository.ModeRepository
import io.github.uveshm003.anchor.core.data.repository.SettingsRepository
import io.github.uveshm003.anchor.core.data.repository.asEntity
import io.github.uveshm003.anchor.core.database.TransactionRunner
import io.github.uveshm003.anchor.core.database.dao.EngineDao
import io.github.uveshm003.anchor.core.database.dao.SessionDao
import io.github.uveshm003.anchor.core.database.entity.EmergencyUnlockEntity
import io.github.uveshm003.anchor.core.database.entity.EngineStateEntity
import io.github.uveshm003.anchor.core.database.entity.OutboxEntity
import io.github.uveshm003.anchor.core.database.entity.OutboxStatus
import io.github.uveshm003.anchor.core.engine.Command
import io.github.uveshm003.anchor.core.engine.CommandExecutor
import io.github.uveshm003.anchor.core.engine.Engine
import io.github.uveshm003.anchor.core.engine.EngineState
import io.github.uveshm003.anchor.core.engine.Environment
import io.github.uveshm003.anchor.core.engine.ExecutionResult
import io.github.uveshm003.anchor.core.engine.Input
import io.github.uveshm003.anchor.core.engine.Outcome
import io.github.uveshm003.anchor.core.engine.ReconcileCause
import io.github.uveshm003.anchor.core.engine.SessionInputs
import io.github.uveshm003.anchor.core.engine.Transition
import io.github.uveshm003.anchor.core.model.SessionEnd
import io.github.uveshm003.anchor.core.model.SessionId
import java.time.Clock
import kotlin.reflect.KClass
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Runs the engine (ADR 0003 §2–3): [submit] only enqueues; one consumer takes each input, samples the
 * [Environment] once, reduces, persists the new state **and** its commands (outbox) in one transaction, then
 * executes the pending commands in order. A failed command is reduced as [Input.CommandFailed] before the next
 * input. Pending commands left by a crash run again on [start].
 */
class SessionController(
    private val scope: CoroutineScope,
    private val clock: Clock,
    private val transaction: TransactionRunner,
    private val engineDao: EngineDao,
    private val sessionDao: SessionDao,
    private val modes: ModeRepository,
    private val anchors: AnchorRepository,
    private val settings: SettingsRepository,
    private val essentialApps: EssentialApps,
    executors: Set<CommandExecutor>,
) : SessionInputs {
    private val registry: Map<KClass<out Command>, CommandExecutor> = buildRegistry(executors)
    private val queue = Channel<Queued>(Channel.UNLIMITED)
    private val _outcomes = MutableSharedFlow<Outcome>(extraBufferCapacity = OUTCOME_BUFFER)

    /** Every outcome, for UI that reacts to taps it didn't submit (e.g. "Tap your Desk anchor"). */
    val outcomes: SharedFlow<Outcome> = _outcomes

    private class Queued(val input: Input, val result: CompletableDeferred<Outcome>?)

    /** Starts the consumer: re-runs pending commands, then reconciles (app start). Call once. */
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
    }

    override suspend fun submit(input: Input) {
        queue.send(Queued(input, null))
    }

    /** Submits [input] and waits for its outcome (for the UI's own actions). */
    suspend fun request(input: Input): Outcome {
        val result = CompletableDeferred<Outcome>()
        queue.send(Queued(input, result))
        return result.await()
    }

    private suspend fun process(input: Input): Outcome {
        val transition = Engine.reduce(loadState(), input, environment())
        persist(transition)
        _outcomes.tryEmit(transition.outcome)
        executePending()
        return transition.outcome
    }

    private suspend fun environment(): Environment {
        val user = settings.settings.first()
        return Environment(
            now = clock.instant(),
            modes = modes.getModes(),
            anchors = anchors.getAnchors(),
            defaultModeId = user.defaultModeId,
            essentials = essentialApps.packages(includeDefaultSms = user.allowDefaultSmsApp),
            emergencyQuotaPerWeek = user.emergencyQuotaPerWeek,
        )
    }

    private suspend fun loadState(): EngineState =
        engineDao.getState()?.let { EngineJson.decodeState(it.json) } ?: EngineState()

    /** State, outbox rows and session history in one transaction: persist **before** side effects. */
    private suspend fun persist(transition: Transition) = transaction {
        engineDao.putState(EngineStateEntity(json = EngineJson.encodeState(transition.state)))
        val first = engineDao.lastSequence() + 1
        engineDao.pruneDone(beforeSequence = first)
        engineDao.insertCommands(
            transition.commands.mapIndexed { index, command ->
                OutboxEntity(command.id, first + index, EngineJson.encodeCommand(command), OutboxStatus.Pending)
            },
        )
        when (val outcome = transition.outcome) {
            is Outcome.Started -> sessionDao.upsert(outcome.session.asEntity())

            is Outcome.Ended -> {
                sessionDao.upsert(outcome.session.asEntity())
                if (outcome.session.endedBy == SessionEnd.EmergencyUnlock) {
                    sessionDao.insertUnlock(
                        EmergencyUnlockEntity(sessionId = outcome.session.id.value, at = clock.millis()),
                    )
                }
            }

            else -> Unit
        }
    }

    private suspend fun executePending() {
        for (row in engineDao.pendingCommands()) {
            val command = EngineJson.decodeCommand(row.json)
            when (val result = registry.getValue(command::class).execute(command)) {
                ExecutionResult.Done -> engineDao.setStatus(row.commandId, OutboxStatus.Done)

                is ExecutionResult.Failed -> {
                    engineDao.setStatus(row.commandId, OutboxStatus.Failed)
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
        is Command.ClearAppPolicy -> null
    }

    companion object {
        private const val OUTCOME_BUFFER = 16

        /** Every command type the engine emits; each needs exactly one executor. */
        val COMMAND_TYPES: Set<KClass<out Command>> = setOf(
            Command.ApplyAppPolicy::class,
            Command.ClearAppPolicy::class,
            Command.ScheduleTimer::class,
            Command.CancelTimer::class,
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
