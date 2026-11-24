package io.github.uveshm003.anchor.feature.anchors

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.uveshm003.anchor.core.data.repository.AnchorRepository
import io.github.uveshm003.anchor.core.data.repository.ModeRepository
import io.github.uveshm003.anchor.core.model.Anchor
import io.github.uveshm003.anchor.core.model.AnchorId
import io.github.uveshm003.anchor.core.model.AnchorType
import io.github.uveshm003.anchor.core.model.ExitRule
import io.github.uveshm003.anchor.core.nfc.AnchorTags
import io.github.uveshm003.anchor.core.nfc.NfcAvailability
import io.github.uveshm003.anchor.core.nfc.NfcStatus
import io.github.uveshm003.anchor.core.nfc.WriteResult
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface Pairing {
    data object Idle : Pairing

    /** Waiting for the user to hold the phone to a sticker. */
    data object Waiting : Pairing

    /** A new sticker: ask for its name. */
    data class Found(val hash: String, val write: WriteResult) : Pairing

    data class AlreadyPaired(val name: String) : Pairing
}

data class AnchorsUiState(
    val loading: Boolean = true,
    val anchors: List<Anchor> = emptyList(),
    val nfc: NfcAvailability = NfcAvailability.Enabled,
    val pairing: Pairing = Pairing.Idle,
)

@HiltViewModel
class AnchorsViewModel @Inject constructor(
    private val repository: AnchorRepository,
    private val modes: ModeRepository,
    private val tags: AnchorTags,
    private val nfcStatus: NfcStatus,
) : ViewModel() {
    private val nfc = MutableStateFlow(nfcStatus.current())
    private val pairing = MutableStateFlow<Pairing>(Pairing.Idle)

    val uiState: StateFlow<AnchorsUiState> = combine(repository.anchors, nfc, pairing) { anchors, nfc, pairing ->
        AnchorsUiState(loading = false, anchors = anchors, nfc = nfc, pairing = pairing)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AnchorsUiState())

    init {
        viewModelScope.launch {
            tags.scanned.collect { scanned ->
                tags.setPairing(false)
                val existing = repository.getAnchors().find { it.hash == scanned.hash }
                pairing.value =
                    existing?.let { Pairing.AlreadyPaired(it.name) } ?: Pairing.Found(scanned.hash, scanned.write)
            }
        }
    }

    /** Re-check NFC (e.g. after returning from settings). */
    fun refreshNfc() {
        nfc.value = nfcStatus.current()
    }

    fun startPairing() {
        tags.setPairing(true)
        pairing.value = Pairing.Waiting
    }

    fun dismissPairing() {
        tags.setPairing(false)
        pairing.value = Pairing.Idle
    }

    fun save(name: String) {
        val found = pairing.value as? Pairing.Found ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.upsert(Anchor(AnchorId(0), name.trim(), AnchorType.Nfc, found.hash))
            pairing.value = Pairing.Idle
        }
    }

    fun rename(anchor: Anchor, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.upsert(anchor.copy(name = name.trim())) }
    }

    /** Unpairs [anchor]; modes that required it fall back to any anchor, so no mode is left impossible to end. */
    fun unpair(anchor: Anchor) {
        viewModelScope.launch {
            modes.getModes()
                .filter { (it.exitRule as? ExitRule.RequiresAnchor)?.anchorId == anchor.id }
                .forEach { modes.upsert(it.copy(exitRule = ExitRule.RequiresAnchor())) }
            repository.delete(anchor.id)
        }
    }

    override fun onCleared() {
        tags.setPairing(false)
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
