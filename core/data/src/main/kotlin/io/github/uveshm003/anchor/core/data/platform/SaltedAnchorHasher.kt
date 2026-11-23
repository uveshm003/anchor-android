package io.github.uveshm003.anchor.core.data.platform

import io.github.uveshm003.anchor.core.datastore.SettingsDataSource
import io.github.uveshm003.anchor.core.engine.AnchorHasher
import java.security.MessageDigest
import javax.inject.Inject

/** SHA-256 over the per-install salt and the tag UID / QR secret; only this hash is ever stored. */
internal class SaltedAnchorHasher @Inject constructor(private val settings: SettingsDataSource) : AnchorHasher {
    override suspend fun hash(secret: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(settings.installSalt().encodeToByteArray())
        digest.update(secret)
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
