package io.github.uveshm003.anchor.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import io.github.uveshm003.anchor.core.data.platform.AndroidBlockerStatus
import io.github.uveshm003.anchor.core.data.platform.AndroidEssentialApps
import io.github.uveshm003.anchor.core.data.platform.BlockerStatus
import io.github.uveshm003.anchor.core.data.platform.EssentialApps
import io.github.uveshm003.anchor.core.data.platform.SaltedAnchorHasher
import io.github.uveshm003.anchor.core.data.repository.AnchorRepository
import io.github.uveshm003.anchor.core.data.repository.AndroidInstalledAppsRepository
import io.github.uveshm003.anchor.core.data.repository.InstalledAppsRepository
import io.github.uveshm003.anchor.core.data.repository.ModeRepository
import io.github.uveshm003.anchor.core.data.repository.OfflineAnchorRepository
import io.github.uveshm003.anchor.core.data.repository.OfflineBlockAttemptRecorder
import io.github.uveshm003.anchor.core.data.repository.OfflineModeRepository
import io.github.uveshm003.anchor.core.data.repository.OfflineSessionRepository
import io.github.uveshm003.anchor.core.data.repository.OfflineSettingsRepository
import io.github.uveshm003.anchor.core.data.repository.OfflineUnlockHistoryRepository
import io.github.uveshm003.anchor.core.data.repository.SessionRepository
import io.github.uveshm003.anchor.core.data.repository.SettingsRepository
import io.github.uveshm003.anchor.core.data.repository.UnlockHistoryRepository
import io.github.uveshm003.anchor.core.data.session.SessionController
import io.github.uveshm003.anchor.core.data.session.SessionRequests
import io.github.uveshm003.anchor.core.engine.AnchorHasher
import io.github.uveshm003.anchor.core.engine.BlockAttemptRecorder
import io.github.uveshm003.anchor.core.engine.CommandExecutor
import io.github.uveshm003.anchor.core.engine.SessionInputs

/** Repositories over Room and DataStore. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {
    @Binds abstract fun modes(impl: OfflineModeRepository): ModeRepository

    @Binds abstract fun anchors(impl: OfflineAnchorRepository): AnchorRepository

    @Binds abstract fun sessions(impl: OfflineSessionRepository): SessionRepository

    @Binds abstract fun unlocks(impl: OfflineUnlockHistoryRepository): UnlockHistoryRepository

    @Binds abstract fun settings(impl: OfflineSettingsRepository): SettingsRepository

    @Binds abstract fun installedApps(impl: AndroidInstalledAppsRepository): InstalledAppsRepository
}

/** The session controller, its ports and platform sources. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class SessionModule {
    @Binds abstract fun essentials(impl: AndroidEssentialApps): EssentialApps

    @Binds abstract fun hasher(impl: SaltedAnchorHasher): AnchorHasher

    @Binds abstract fun blockAttempts(impl: OfflineBlockAttemptRecorder): BlockAttemptRecorder

    @Binds abstract fun inputs(controller: SessionController): SessionInputs

    @Binds abstract fun requests(controller: SessionController): SessionRequests

    @Binds abstract fun blockerStatus(impl: AndroidBlockerStatus): BlockerStatus

    /** Adapters contribute executors with `@IntoSet`; declared so the set may be empty in tests. */
    @Multibinds abstract fun executors(): Set<CommandExecutor>
}
