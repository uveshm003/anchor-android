package io.github.uveshm003.anchor.core.data.platform

import android.content.Context
import android.os.Build
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Phone settings that decide whether Anchor works reliably. */
interface DeviceSetup {
    /** Anchor is subject to battery optimisation (not on the system's "unrestricted" list). */
    fun batteryOptimized(): Boolean

    /**
     * Installed outside an app store. On Android 13+ such apps need "Allow restricted settings" before their
     * Accessibility service can be turned on.
     */
    fun sideloaded(): Boolean

    /** Lower-case manufacturer, for OEM-specific battery hints. */
    fun manufacturer(): String
}

internal class AndroidDeviceSetup @Inject constructor(@ApplicationContext private val context: Context) : DeviceSetup {
    override fun batteryOptimized(): Boolean =
        context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) != true

    /** https://developer.android.com/reference/android/content/pm/InstallSourceInfo (checked 2026-11-24). */
    override fun sideloaded(): Boolean {
        val installer = context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        return installer == null || installer in PACKAGE_INSTALLERS
    }

    override fun manufacturer(): String = Build.MANUFACTURER.orEmpty().lowercase()

    private companion object {
        /** The system's own installer: an APK opened from a file or browser. */
        val PACKAGE_INSTALLERS = setOf("com.google.android.packageinstaller", "com.android.packageinstaller")
    }
}
