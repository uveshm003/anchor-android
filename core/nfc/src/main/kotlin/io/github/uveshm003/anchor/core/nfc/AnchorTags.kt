package io.github.uveshm003.anchor.core.nfc

import io.github.uveshm003.anchor.core.common.dispatchers.ApplicationScope
import io.github.uveshm003.anchor.core.engine.AnchorHasher
import io.github.uveshm003.anchor.core.engine.Input
import io.github.uveshm003.anchor.core.engine.SessionInputs
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** What pairing learned from a tag: its salted hash and whether Anchor's NDEF record was written to it. */
data class ScannedTag(val hash: String, val write: WriteResult)

enum class WriteResult {
    /** URI + Android Application Record written: background taps open Anchor. */
    Written,

    /** Read-only or not NDEF: works while Anchor is open (UID only). */
    NotWritable,

    /** Nothing to write (simulated tap) or writing failed. */
    Skipped,
}

/**
 * Routes every tag read (reader mode, background NDEF dispatch, or the debug simulator) to pairing when the
 * Anchors screen is pairing, otherwise to the engine as [Input.AnchorTapped]. Only the salted hash leaves here.
 */
@Singleton
class AnchorTags @Inject constructor(
    private val hasher: AnchorHasher,
    private val inputs: SessionInputs,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val pairingState = MutableStateFlow(false)
    val pairing: StateFlow<Boolean> = pairingState

    private val scannedFlow = MutableSharedFlow<ScannedTag>(extraBufferCapacity = 1)

    /** Tags read while pairing. */
    val scanned: SharedFlow<ScannedTag> = scannedFlow

    fun setPairing(pairing: Boolean) {
        pairingState.value = pairing
    }

    /** [write] tries to write Anchor's NDEF record; it is only called while pairing. */
    fun onTag(uid: ByteArray, write: () -> WriteResult = { WriteResult.Skipped }) {
        val pairing = pairingState.value
        val result = if (pairing) write() else WriteResult.Skipped
        scope.launch {
            val hash = hasher.hash(uid)
            if (pairing) scannedFlow.emit(ScannedTag(hash, result)) else inputs.submit(Input.AnchorTapped(hash))
        }
    }
}
