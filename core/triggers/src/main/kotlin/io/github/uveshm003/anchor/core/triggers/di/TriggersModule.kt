package io.github.uveshm003.anchor.core.triggers.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.github.uveshm003.anchor.core.engine.CommandExecutor
import io.github.uveshm003.anchor.core.triggers.timer.TimerExecutor

@Module
@InstallIn(SingletonComponent::class)
internal abstract class TriggersModule {
    @Binds @IntoSet
    abstract fun timer(executor: TimerExecutor): CommandExecutor
}
