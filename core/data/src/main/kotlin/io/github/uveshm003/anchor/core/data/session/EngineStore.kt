package io.github.uveshm003.anchor.core.data.session

import io.github.uveshm003.anchor.core.data.repository.asEntity
import io.github.uveshm003.anchor.core.database.TransactionRunner
import io.github.uveshm003.anchor.core.database.dao.EngineDao
import io.github.uveshm003.anchor.core.database.dao.SessionDao
import io.github.uveshm003.anchor.core.database.entity.EmergencyUnlockEntity
import io.github.uveshm003.anchor.core.database.entity.EngineStateEntity
import io.github.uveshm003.anchor.core.database.entity.OutboxEntity
import io.github.uveshm003.anchor.core.database.entity.OutboxStatus
import io.github.uveshm003.anchor.core.engine.Command
import io.github.uveshm003.anchor.core.engine.EngineState
import io.github.uveshm003.anchor.core.engine.Outcome
import io.github.uveshm003.anchor.core.engine.Transition
import io.github.uveshm003.anchor.core.model.SessionEnd
import javax.inject.Inject

/** Engine state, command outbox and session history. */
class EngineStore @Inject constructor(
    private val transaction: TransactionRunner,
    private val engineDao: EngineDao,
    private val sessionDao: SessionDao,
) {
    suspend fun load(): EngineState = engineDao.getState()?.let { EngineJson.decodeState(it.json) } ?: EngineState()

    /** State, outbox rows and session history in one transaction: persisted **before** any side effect. */
    suspend fun persist(transition: Transition) = transaction {
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

            is Outcome.Switched -> {
                sessionDao.upsert(outcome.ended.asEntity())
                sessionDao.upsert(outcome.started.asEntity())
            }

            is Outcome.Ended -> {
                val session = outcome.session
                sessionDao.upsert(session.asEntity())
                if (session.endedBy == SessionEnd.EmergencyUnlock) {
                    sessionDao.insertUnlock(
                        EmergencyUnlockEntity(sessionId = session.id.value, at = session.endedAt!!.toEpochMilli()),
                    )
                }
            }

            else -> Unit
        }
    }

    suspend fun pending(): List<Pair<String, Command>> =
        engineDao.pendingCommands().map { it.commandId to EngineJson.decodeCommand(it.json) }

    suspend fun markDone(commandId: String) = engineDao.setStatus(commandId, OutboxStatus.Done)

    suspend fun markFailed(commandId: String) = engineDao.setStatus(commandId, OutboxStatus.Failed)
}
