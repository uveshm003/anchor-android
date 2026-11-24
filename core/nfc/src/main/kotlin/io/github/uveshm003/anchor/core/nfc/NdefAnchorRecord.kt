package io.github.uveshm003.anchor.core.nfc

import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.util.Log
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
            Ndef.get(tag)?.let { write(it, message) }
                ?: NdefFormatable.get(tag)?.let { format(it, message) }
                ?: WriteResult.NotWritable
        } catch (e: IOException) {
            failed(e)
        } catch (e: FormatException) {
            failed(e)
        }
    }

    private fun write(ndef: Ndef, message: NdefMessage): WriteResult = ndef.use {
        it.connect()
        if (!it.isWritable || it.maxSize < message.byteArrayLength) {
            WriteResult.NotWritable
        } else {
            it.writeNdefMessage(message)
            WriteResult.Written
        }
    }

    private fun format(formatable: NdefFormatable, message: NdefMessage): WriteResult = formatable.use {
        it.connect()
        it.format(message)
        WriteResult.Written
    }

    /** The tag moved away or refused the write: it still pairs by UID. */
    private fun failed(e: Exception): WriteResult {
        Log.w(TAG, "Could not write Anchor's NDEF record", e)
        return WriteResult.Skipped
    }

    private const val TAG = "NdefAnchorRecord"
}
