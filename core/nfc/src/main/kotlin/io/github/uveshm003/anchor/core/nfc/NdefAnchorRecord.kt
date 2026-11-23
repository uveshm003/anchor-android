package io.github.uveshm003.anchor.core.nfc

import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import java.io.IOException

/**
 * Anchor's NDEF message: an app URI (matched by the background `NDEF_DISCOVERED` filter) plus an Android
 * Application Record, so a tap opens Anchor even when it isn't running. The tag's identity is still its UID
 * hash: the URI alone never counts as an anchor tap.
 */
object NdefAnchorRecord {
    const val SCHEME = "io.github.uveshm003.anchor"
    const val HOST = "tap"

    fun message(packageName: String): NdefMessage = NdefMessage(
        NdefRecord.createUri("$SCHEME://$HOST"),
        NdefRecord.createApplicationRecord(packageName),
    )

    fun write(tag: Tag, packageName: String): WriteResult {
        val message = message(packageName)
        return try {
            val ndef = Ndef.get(tag)
            val formatable = NdefFormatable.get(tag)
            when {
                ndef != null -> ndef.use {
                    it.connect()
                    if (!it.isWritable || it.maxSize < message.byteArrayLength) return WriteResult.NotWritable
                    it.writeNdefMessage(message)
                    WriteResult.Written
                }

                formatable != null -> formatable.use {
                    it.connect()
                    it.format(message)
                    WriteResult.Written
                }

                else -> WriteResult.NotWritable
            }
        } catch (e: IOException) {
            WriteResult.Skipped.also { e.printStackTrace() }
        } catch (e: FormatException) {
            WriteResult.Skipped.also { e.printStackTrace() }
        }
    }
}
