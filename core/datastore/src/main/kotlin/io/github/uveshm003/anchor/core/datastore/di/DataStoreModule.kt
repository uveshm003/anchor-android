package io.github.uveshm003.anchor.core.datastore.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.uveshm003.anchor.core.common.dispatchers.AnchorDispatchers
import io.github.uveshm003.anchor.core.common.dispatchers.ApplicationScope
import io.github.uveshm003.anchor.core.common.dispatchers.Dispatcher
import io.github.uveshm003.anchor.core.datastore.AnchorSettings
import io.github.uveshm003.anchor.core.datastore.AnchorSettingsSerializer
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.plus

@Module
@InstallIn(SingletonComponent::class)
internal object DataStoreModule {
    @Provides
    @Singleton
    fun settingsStore(
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope,
        @Dispatcher(AnchorDispatchers.IO) io: CoroutineDispatcher,
    ): DataStore<AnchorSettings> = settingsDataStore(scope + io) { context.dataStoreFile("settings.json") }
}

/** Corrupt data falls back to defaults instead of crashing. */
internal fun settingsDataStore(scope: CoroutineScope, file: () -> java.io.File): DataStore<AnchorSettings> =
    DataStoreFactory.create(
        serializer = AnchorSettingsSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler { AnchorSettings() },
        scope = scope,
        produceFile = file,
    )
