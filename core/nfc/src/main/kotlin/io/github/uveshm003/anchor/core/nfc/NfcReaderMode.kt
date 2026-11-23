package io.github.uveshm003.anchor.core.nfc

import android.app.Activity
import android.nfc.NfcAdapter
import android.os.Bundle

/** Foreground reader mode: while Anchor is open, taps come here instead of the system's tag dispatch. */
object NfcReaderMode {
    private const val FLAGS = NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
        NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V or NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS

    fun enable(activity: Activity, tags: AnchorTags) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        adapter.enableReaderMode(
            activity,
            { tag -> tags.onTag(tag.id) { NdefAnchorRecord.write(tag, activity.packageName) } },
            FLAGS,
            Bundle(),
        )
    }

    fun disable(activity: Activity) {
        NfcAdapter.getDefaultAdapter(activity)?.disableReaderMode(activity)
    }
}

enum class NfcAvailability { Unavailable, Disabled, Enabled }

fun nfcAvailability(context: android.content.Context): NfcAvailability {
    val adapter = NfcAdapter.getDefaultAdapter(context) ?: return NfcAvailability.Unavailable
    return if (adapter.isEnabled) NfcAvailability.Enabled else NfcAvailability.Disabled
}

/** Current NFC availability (a function so screens can re-check on resume and tests can fake it). */
fun interface NfcStatus {
    fun current(): NfcAvailability
}

class AndroidNfcStatus @javax.inject.Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
) : NfcStatus {
    override fun current(): NfcAvailability = nfcAvailability(context)
}
