package io.github.uveshm003.anchor.core.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** JSON on disk (no protobuf dependency). Unreadable data raises [CorruptionException]; the handler resets it. */
internal object AnchorSettingsSerializer : Serializer<AnchorSettings> {
    private val json = Json { ignoreUnknownKeys = true }

    override val defaultValue: AnchorSettings = AnchorSettings()

    override suspend fun readFrom(input: InputStream): AnchorSettings = try {
        json.decodeFromString(input.readBytes().decodeToString())
    } catch (e: SerializationException) {
        throw CorruptionException("Unreadable settings", e)
    } catch (e: IllegalArgumentException) {
        throw CorruptionException("Unreadable settings", e)
    }

    override suspend fun writeTo(t: AnchorSettings, output: OutputStream) {
        output.write(json.encodeToString(t).encodeToByteArray())
    }
}
