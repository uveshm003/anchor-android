package io.github.uveshm003.anchor.feature.settings

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.uveshm003.anchor.core.data.platform.BlockerStatus
import io.github.uveshm003.anchor.core.data.platform.DeviceSetup
import io.github.uveshm003.anchor.core.nfc.NfcAvailability
import io.github.uveshm003.anchor.core.nfc.NfcStatus
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** OEMs known to stop background apps aggressively (https://dontkillmyapp.com). */
enum class Oem(val slug: String) {
    Xiaomi(
        "xiaomi",
    ),
    Huawei(
        "huawei",
    ),
    Honor("honor"),
    Oppo("oppo"),
    Vivo("vivo"),
    Realme("realme"),
    OnePlus("oneplus"),
    Samsung("samsung"),
}

data class SetupState(
    val blockerEnabled: Boolean = true,
    /** Sideloaded on Android 13+: "Allow restricted settings" comes first. */
    val needsRestrictedSettings: Boolean = false,
    val batteryOptimized: Boolean = false,
    val oem: Oem? = null,
    val nfc: NfcAvailability = NfcAvailability.Enabled,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    blocker: BlockerStatus,
    private val device: DeviceSetup,
    private val nfcStatus: NfcStatus,
) : ViewModel() {
    private val checks = MutableStateFlow(snapshot())

    val uiState: StateFlow<SetupState> = combine(blocker.enabled, checks) { enabled, state ->
        state.copy(blockerEnabled = enabled)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), checks.value)

    /** Re-check after returning from a settings screen. */
    fun refresh() {
        checks.value = snapshot()
    }

    private fun snapshot() = SetupState(
        needsRestrictedSettings = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && device.sideloaded(),
        batteryOptimized = device.batteryOptimized(),
        oem = Oem.entries.find { device.manufacturer().contains(it.slug) },
        nfc = nfcStatus.current(),
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
