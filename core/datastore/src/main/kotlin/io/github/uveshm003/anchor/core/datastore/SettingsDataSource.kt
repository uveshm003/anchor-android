package io.github.uveshm003.anchor.core.datastore

import androidx.datastore.core.DataStore
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** Reads and writes [AnchorSettings]. Business rules (e.g. no quota change mid-session) live in `core:data`. */
@Singleton
class SettingsDataSource @Inject constructor(private val store: DataStore<AnchorSettings>) {
    val settings: Flow<AnchorSettings> = store.data

    suspend fun update(transform: (AnchorSettings) -> AnchorSettings) {
        store.updateData(transform)
    }

    /** The per-install salt, created once with a secure random source. */
    suspend fun installSalt(): String {
        store.data.first().installSalt?.let { return it }
        return store.updateData { current ->
            if (current.installSalt != null) current else current.copy(installSalt = newSalt())
        }.installSalt!!
    }

    private fun newSalt(): String {
        val bytes = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val SALT_BYTES = 32
    }
}
