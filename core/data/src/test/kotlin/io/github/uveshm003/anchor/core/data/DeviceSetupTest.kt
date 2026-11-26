package io.github.uveshm003.anchor.core.data

import android.app.Application
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.uveshm003.anchor.core.data.platform.AndroidDeviceSetup
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class DeviceSetupTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val setup = AndroidDeviceSetup(context)

    @Test
    fun battery_optimisation_follows_the_power_manager() {
        val power = shadowOf(context.getSystemService(PowerManager::class.java))
        power.setIgnoringBatteryOptimizations(context.packageName, false)
        assertThat(setup.batteryOptimized()).isTrue()
        power.setIgnoringBatteryOptimizations(context.packageName, true)
        assertThat(setup.batteryOptimized()).isFalse()
    }

    @Test
    fun sideloaded_when_installed_by_the_package_installer_or_unknown() {
        val pm = shadowOf(context.packageManager)
        pm.setInstallSourceInfo(context.packageName, null, null)
        assertThat(setup.sideloaded()).isTrue()
        pm.setInstallSourceInfo(context.packageName, "com.android.vending", "com.android.vending")
        assertThat(setup.sideloaded()).isFalse()
        pm.setInstallSourceInfo(
            context.packageName,
            "com.google.android.packageinstaller",
            "com.google.android.packageinstaller",
        )
        assertThat(setup.sideloaded()).isTrue()
        assertThat(setup.manufacturer()).isEqualTo(setup.manufacturer().lowercase())
    }
}
