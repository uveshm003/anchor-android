package io.github.uveshm003.anchor.feature.home.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.uveshm003.anchor.core.data.session.SessionRequests
import io.github.uveshm003.anchor.core.engine.AnchorHasher
import io.github.uveshm003.anchor.core.engine.IgnoreReason
import io.github.uveshm003.anchor.core.engine.Input
import io.github.uveshm003.anchor.core.engine.Outcome
import io.github.uveshm003.anchor.core.engine.RejectReason
import io.github.uveshm003.anchor.core.qr.QrAnchorCode
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface ScanState {
    data object Scanning : ScanState

    /** A QR code that isn't an Anchor code is in view. */
    data object NotAnAnchor : ScanState

    data class Started(val modeName: String) : ScanState

    data object Ended : ScanState

    /** This session needs a specific anchor (named when known). */
    data class WrongAnchor(val name: String?) : ScanState

    data object Unknown : ScanState

    data object Rejected : ScanState
}

/** A scanned QR anchor acts exactly like an NFC tap: same salted hash, same engine rules (ADR 0008). */
@HiltViewModel
class ScanViewModel @Inject constructor(private val hasher: AnchorHasher, private val requests: SessionRequests) :
    ViewModel() {
    private val _state = MutableStateFlow<ScanState>(ScanState.Scanning)
    val state: StateFlow<ScanState> = _state

    private var handling = false

    /** Called from the camera analyzer thread for every decoded QR text. */
    fun onScanned(text: String) {
        if (handling) return
        val secret = QrAnchorCode.decode(text)
        if (secret == null) {
            _state.value = ScanState.NotAnAnchor
            return
        }
        handling = true
        viewModelScope.launch {
            _state.value = when (val outcome = requests.request(Input.AnchorTapped(hasher.hash(secret)))) {
                is Outcome.Started -> ScanState.Started(outcome.session.modeName)

                is Outcome.Ended -> ScanState.Ended

                is Outcome.Ignored -> if (outcome.reason ==
                    IgnoreReason.UnknownAnchor
                ) {
                    ScanState.Unknown
                } else {
                    ScanState.Rejected
                }

                is Outcome.Rejected -> (outcome.reason as? RejectReason.RequiresAnchor)
                    ?.let { ScanState.WrongAnchor(it.anchorName) } ?: ScanState.Rejected

                else -> ScanState.Rejected
            }
        }
    }

    /** Scan again after a result. */
    fun reset() {
        handling = false
        _state.value = ScanState.Scanning
    }
}
