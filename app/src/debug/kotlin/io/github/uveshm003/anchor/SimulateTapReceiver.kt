package io.github.uveshm003.anchor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import io.github.uveshm003.anchor.core.nfc.AnchorTags
import javax.inject.Inject

/**
 * Debug builds only: simulates an NFC tap so flows work without hardware (emulator, CI). Goes through the same
 * path as a real tap (pairing when the Anchors screen is pairing, otherwise the engine). Protected by DUMP,
 * which `adb shell` holds and apps can't get:
 *
 * adb shell am broadcast -n io.github.uveshm003.anchor.debug/io.github.uveshm003.anchor.SimulateTapReceiver \
 *     -a io.github.uveshm003.anchor.SIMULATE_TAP --es uid 04A1B2C3D4E5F6 [--ez pair true]
 *
 * `pair true` pairs the tag (for emulators, which have no NFC to start pairing from the Anchors screen).
 */
@AndroidEntryPoint
class SimulateTapReceiver : BroadcastReceiver() {
    @Inject lateinit var tags: AnchorTags

    override fun onReceive(context: Context, intent: Intent) {
        val hex = intent.getStringExtra(EXTRA_UID)?.filter { it.isLetterOrDigit() } ?: return
        if (hex.isEmpty() || hex.length % 2 != 0) return
        if (intent.getBooleanExtra(EXTRA_PAIR, false)) tags.setPairing(true)
        tags.onTag(hex.chunked(2).map { it.toInt(HEX).toByte() }.toByteArray())
    }

    private companion object {
        const val EXTRA_UID = "uid"
        const val EXTRA_PAIR = "pair"
        const val HEX = 16
    }
}
