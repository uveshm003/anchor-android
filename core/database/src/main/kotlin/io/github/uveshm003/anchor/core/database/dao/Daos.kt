package io.github.uveshm003.anchor.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import io.github.uveshm003.anchor.core.database.entity.AnchorEntity
import io.github.uveshm003.anchor.core.database.entity.BlockAttemptEntity
import io.github.uveshm003.anchor.core.database.entity.EmergencyUnlockEntity
import io.github.uveshm003.anchor.core.database.entity.EngineStateEntity
import io.github.uveshm003.anchor.core.database.entity.ModeEntity
import io.github.uveshm003.anchor.core.database.entity.OutboxEntity
import io.github.uveshm003.anchor.core.database.entity.OutboxStatus
import io.github.uveshm003.anchor.core.database.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModeDao {
    @Query("SELECT * FROM modes ORDER BY priority, id")
    fun observeAll(): Flow<List<ModeEntity>>

    @Query("SELECT * FROM modes ORDER BY priority, id")
    suspend fun getAll(): List<ModeEntity>

    @Query("SELECT * FROM modes WHERE id = :id")
    suspend fun get(id: Long): ModeEntity?

    @Upsert
    suspend fun upsert(mode: ModeEntity): Long

    @Query("DELETE FROM modes WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE modes SET priority = :priority WHERE id = :id")
    suspend fun setPriority(id: Long, priority: Int)

    /** Priorities become the list order (0 = first), atomically. */
    @Transaction
    suspend fun reorder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> setPriority(id, index) }
    }
}

@Dao
interface AnchorDao {
    @Query("SELECT * FROM anchors ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<AnchorEntity>>

    @Query("SELECT * FROM anchors")
    suspend fun getAll(): List<AnchorEntity>

    @Upsert
    suspend fun upsert(anchor: AnchorEntity): Long

    @Query("DELETE FROM anchors WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeActive(): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE endedAt IS NOT NULL ORDER BY startedAt DESC")
    fun observeHistory(): Flow<List<SessionEntity>>

    @Upsert
    suspend fun upsert(session: SessionEntity)

    @Insert
    suspend fun insertBlockAttempt(attempt: BlockAttemptEntity)

    @Query("UPDATE sessions SET blockAttempts = blockAttempts + 1 WHERE id = :sessionId")
    suspend fun incrementBlockAttempts(sessionId: Long)

    @Insert
    suspend fun insertUnlock(unlock: EmergencyUnlockEntity)

    @Query("SELECT COUNT(*) FROM emergency_unlocks WHERE at > :sinceMillis")
    fun observeUnlockCountSince(sinceMillis: Long): Flow<Int>
}

@Dao
interface EngineDao {
    @Query("SELECT * FROM engine_state WHERE id = 0")
    suspend fun getState(): EngineStateEntity?

    @Query("SELECT * FROM engine_state WHERE id = 0")
    fun observeState(): Flow<EngineStateEntity?>

    @Upsert
    suspend fun putState(state: EngineStateEntity)

    @Insert
    suspend fun insertCommands(commands: List<OutboxEntity>)

    @Query("SELECT * FROM outbox WHERE status = 'Pending' ORDER BY sequence")
    suspend fun pendingCommands(): List<OutboxEntity>

    @Query("SELECT COALESCE(MAX(sequence), 0) FROM outbox")
    suspend fun lastSequence(): Long

    @Query("UPDATE outbox SET status = :status WHERE commandId = :commandId")
    suspend fun setStatus(commandId: String, status: OutboxStatus)

    /** Done commands are only needed for dedupe until the next transition; keep the table small. */
    @Query("DELETE FROM outbox WHERE status = 'Done' AND sequence < :beforeSequence")
    suspend fun pruneDone(beforeSequence: Long)
}
