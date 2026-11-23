package io.github.uveshm003.anchor.service.blocker.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.github.uveshm003.anchor.core.engine.CommandExecutor
import io.github.uveshm003.anchor.service.blocker.policy.PolicyExecutor

@Module
@InstallIn(SingletonComponent::class)
internal abstract class BlockerModule {
    @Binds @IntoSet
    abstract fun policy(executor: PolicyExecutor): CommandExecutor
}
