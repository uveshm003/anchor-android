package io.github.uveshm003.anchor.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.uveshm003.anchor.core.database.dao.AnchorDao
import io.github.uveshm003.anchor.core.database.dao.EngineDao
import io.github.uveshm003.anchor.core.database.dao.ModeDao
import io.github.uveshm003.anchor.core.database.dao.SessionDao
import io.github.uveshm003.anchor.core.database.entity.AnchorEntity
import io.github.uveshm003.anchor.core.database.entity.BlockAttemptEntity
import io.github.uveshm003.anchor.core.database.entity.EmergencyUnlockEntity
import io.github.uveshm003.anchor.core.database.entity.EngineStateEntity
import io.github.uveshm003.anchor.core.database.entity.ModeEntity
import io.github.uveshm003.anchor.core.database.entity.OutboxEntity
import io.github.uveshm003.anchor.core.database.entity.SessionEntity

/** Schema v1 (exported to `core/database/schemas`). Every later version needs a migration and a migration test. */
@Database(
    entities = [
        ModeEntity::class,
        AnchorEntity::class,
        SessionEntity::class,
        BlockAttemptEntity::class,
        EmergencyUnlockEntity::class,
        EngineStateEntity::class,
        OutboxEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AnchorDatabase : RoomDatabase() {
    abstract fun modeDao(): ModeDao

    abstract fun anchorDao(): AnchorDao

    abstract fun sessionDao(): SessionDao

    abstract fun engineDao(): EngineDao

    companion object {
        const val NAME = "anchor.db"
    }
}
