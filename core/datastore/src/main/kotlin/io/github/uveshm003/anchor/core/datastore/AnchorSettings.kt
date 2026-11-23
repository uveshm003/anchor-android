package io.github.uveshm003.anchor.core.datastore

import kotlinx.serialization.Serializable

/** Small user settings (DataStore). Defaults apply to a fresh install and to unreadable data. */
@Serializable
data class AnchorSettings(
    /** The default SMS app counts as essential (never blocked). */
    val allowDefaultSmsApp: Boolean = true,
    val emergencyQuotaPerWeek: Int = DEFAULT_EMERGENCY_QUOTA,
    val onboardingCompleted: Boolean = false,
    /** Mode an unbound anchor starts when idle. */
    val defaultModeId: Long? = null,
    /** Per-install salt for hashing anchor UIDs (hex); generated on first use, never leaves the device. */
    val installSalt: String? = null,
) {
    companion object {
        const val DEFAULT_EMERGENCY_QUOTA = 3
        val QUOTA_RANGE = 0..7
    }
}
