package io.github.uveshm003.anchor.service.notifications.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import io.github.uveshm003.anchor.core.engine.CommandExecutor
import io.github.uveshm003.anchor.service.notifications.SessionNotificationExecutor

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NotificationsModule {
    @Binds @IntoSet
    abstract fun sessionNotification(executor: SessionNotificationExecutor): CommandExecutor
}
