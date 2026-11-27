package io.github.uveshm003.anchor.service.blocker.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds
import io.github.uveshm003.anchor.core.engine.CommandExecutor
import io.github.uveshm003.anchor.core.engine.ProcessWatcher
import io.github.uveshm003.anchor.service.blocker.policy.PolicyExecutor

@Module
@InstallIn(SingletonComponent::class)
internal abstract class BlockerModule {
    @Binds @IntoSet
    abstract fun policy(executor: PolicyExecutor): CommandExecutor

    /** Adapters add theirs with `@IntoSet` (e.g. USB in `core:triggers`); empty is fine. */
    @Multibinds
    abstract fun watchers(): Set<ProcessWatcher>
}
