package io.github.uveshm003.anchor.core.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import io.github.uveshm003.anchor.core.common.dispatchers.ApplicationScope
import io.github.uveshm003.anchor.core.data.platform.AndroidEssentialApps
import io.github.uveshm003.anchor.core.data.platform.EssentialApps
import io.github.uveshm003.anchor.core.data.platform.SaltedAnchorHasher
import io.github.uveshm003.anchor.core.data.repository.AnchorRepository
import io.github.uveshm003.anchor.core.data.repository.ModeRepository
import io.github.uveshm003.anchor.core.data.repository.OfflineAnchorRepository
import io.github.uveshm003.anchor.core.data.repository.OfflineModeRepository
import io.github.uveshm003.anchor.core.data.repository.OfflineSessionRepository
import io.github.uveshm003.anchor.core.data.repository.OfflineSettingsRepository
import io.github.uveshm003.anchor.core.data.repository.OfflineUnlockHistoryRepository
import io.github.uveshm003.anchor.core.data.repository.SessionRepository
import io.github.uveshm003.anchor.core.data.repository.SettingsRepository
import io.github.uveshm003.anchor.core.data.repository.UnlockHistoryRepository
import io.github.uveshm003.anchor.core.data.session.SessionController
import io.github.uveshm003.anchor.core.database.TransactionRunner
import io.github.uveshm003.anchor.core.database.dao.EngineDao
import io.github.uveshm003.anchor.core.database.dao.SessionDao
import io.github.uveshm003.anchor.core.engine.AnchorHasher
import io.github.uveshm003.anchor.core.engine.CommandExecutor
import io.github.uveshm003.anchor.core.engine.SessionInputs
import kotlinx.coroutines.CoroutineScope
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds abstract fun modes(impl: OfflineModeRepository): ModeRepository

    @Binds abstract fun anchors(impl: OfflineAnchorRepository): AnchorRepository

    @Binds abstract fun sessions(impl: OfflineSessionRepository): SessionRepository

    @Binds abstract fun unlocks(impl: OfflineUnlockHistoryRepository): UnlockHistoryRepository

    @Binds abstract fun settings(impl: OfflineSettingsRepository): SettingsRepository

    @Binds abstract fun essentials(impl: AndroidEssentialApps): EssentialApps

    @Binds abstract fun hasher(impl: SaltedAnchorHasher): AnchorHasher

    @Binds abstract fun inputs(controller: SessionController): SessionInputs

    /** Adapters contribute executors with `@IntoSet`; declared so the set may be empty in tests. */
    @Multibinds abstract fun executors(): Set<CommandExecutor>

    companion object {
        @Provides
        @Singleton
        fun controller(
            @ApplicationScope scope: CoroutineScope,
            clock: Clock,
            transaction: TransactionRunner,
            engineDao: EngineDao,
            sessionDao: SessionDao,
            modes: ModeRepository,
            anchors: AnchorRepository,
            settings: SettingsRepository,
            essentials: EssentialApps,
            executors: Set<@JvmSuppressWildcards CommandExecutor>,
        ) = SessionController(scope, clock, transaction, engineDao, sessionDao, modes, anchors, settings, essentials, executors)
    }
}
