package io.github.uveshm003.anchor.core.data.platform

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.uveshm003.anchor.core.engine.TriggerPermission
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

    /** Exact alarms are allowed (otherwise schedules may run up to 10 minutes late). */
    fun exactAlarmsAllowed(): Boolean

    /** Whether a trigger's [permission] is granted right now. */
    fun granted(permission: TriggerPermission): Boolean
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

    override fun exactAlarmsAllowed(): Boolean =
        context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true

    override fun granted(permission: TriggerPermission): Boolean = when (permission) {
        TriggerPermission.ExactAlarm -> exactAlarmsAllowed()
        TriggerPermission.Location -> has(Manifest.permission.ACCESS_FINE_LOCATION)
        TriggerPermission.NearbyDevices -> has(Manifest.permission.BLUETOOTH_CONNECT)
        TriggerPermission.ActivityRecognition -> has(Manifest.permission.ACTIVITY_RECOGNITION)
    }

    private fun has(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private companion object {
        /** The system's own installer: an APK opened from a file or browser. */
        val PACKAGE_INSTALLERS = setOf("com.google.android.packageinstaller", "com.android.packageinstaller")
    }
}
