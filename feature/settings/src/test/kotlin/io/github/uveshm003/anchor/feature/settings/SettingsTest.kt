package io.github.uveshm003.anchor.feature.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.uveshm003.anchor.core.data.platform.DeviceSetup
import io.github.uveshm003.anchor.core.nfc.NfcAvailability
import io.github.uveshm003.anchor.core.testing.MainDispatcherRule
import io.github.uveshm003.anchor.core.testing.data.FakeBlockerStatus
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsTest {
    @get:Rule
    val main = MainDispatcherRule()

    private var optimized = true
    private var sideloaded = true
    private var brand = "Xiaomi"
    private var nfc = NfcAvailability.Disabled
    private val device = object : DeviceSetup {
        override fun batteryOptimized() = optimized

        override fun sideloaded() = sideloaded

        override fun manufacturer() = brand.lowercase()

        override fun exactAlarmsAllowed() = true

        override fun granted(permission: io.github.uveshm003.anchor.core.engine.TriggerPermission) = true
    }
    private val blocker = FakeBlockerStatus(enabled = false)

    @Test
    fun reports_each_check_and_refreshes_on_return() = runTest {
        val vm = SettingsViewModel(blocker, device) { nfc }
        vm.uiState.test {
            val first = expectMostRecentItem()
            assertThat(first.blockerEnabled).isFalse()
            assertThat(first.needsRestrictedSettings).isTrue() // Robolectric runs API 33+
            assertThat(first.batteryOptimized).isTrue()
            assertThat(first.oem).isEqualTo(Oem.Xiaomi)
            assertThat(first.nfc).isEqualTo(NfcAvailability.Disabled)

            blocker.enabled.value = true
            optimized = false
            sideloaded = false
            brand = "Google"
            nfc = NfcAvailability.Enabled
            vm.refresh()
            val fixed = expectMostRecentItem()
            assertThat(fixed).isEqualTo(SetupState(blockerEnabled = true))
        }
    }
}
