package io.github.uveshm003.anchor.core.nfc

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Invisible: receives background taps on tags carrying Anchor's NDEF record, forwards the tag UID and finishes.
 * Only a tag object from the system carries a UID; a forged intent without one does nothing, and an unknown
 * UID hash is ignored by the engine.
 */
@AndroidEntryPoint
class AnchorTapActivity : ComponentActivity() {
    @Inject lateinit var tags: AnchorTags

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)?.let { tags.onTag(it.id) }
        finish()
    }
}
