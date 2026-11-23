package io.github.uveshm003.anchor.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.uveshm003.anchor.core.database.AnchorDatabase
import io.github.uveshm003.anchor.core.database.RoomTransactionRunner
import io.github.uveshm003.anchor.core.database.TransactionRunner
import io.github.uveshm003.anchor.core.database.dao.AnchorDao
import io.github.uveshm003.anchor.core.database.dao.EngineDao
import io.github.uveshm003.anchor.core.database.dao.ModeDao
import io.github.uveshm003.anchor.core.database.dao.SessionDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AnchorDatabase =
        Room.databaseBuilder(context, AnchorDatabase::class.java, AnchorDatabase.NAME).build()

    @Provides fun modeDao(db: AnchorDatabase): ModeDao = db.modeDao()

    @Provides fun anchorDao(db: AnchorDatabase): AnchorDao = db.anchorDao()

    @Provides fun sessionDao(db: AnchorDatabase): SessionDao = db.sessionDao()

    @Provides fun engineDao(db: AnchorDatabase): EngineDao = db.engineDao()

    @Provides fun transactionRunner(db: AnchorDatabase): TransactionRunner = RoomTransactionRunner(db)
}
