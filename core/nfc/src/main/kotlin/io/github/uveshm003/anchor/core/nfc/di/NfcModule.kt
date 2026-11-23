package io.github.uveshm003.anchor.core.nfc.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.uveshm003.anchor.core.nfc.AndroidNfcStatus
import io.github.uveshm003.anchor.core.nfc.NfcStatus

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NfcModule {
    @Binds abstract fun status(impl: AndroidNfcStatus): NfcStatus
}
