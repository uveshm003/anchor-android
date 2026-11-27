package io.github.uveshm003.anchor.core.data.session

import io.github.uveshm003.anchor.core.data.platform.DeviceUptime
import io.github.uveshm003.anchor.core.data.platform.EssentialApps
import io.github.uveshm003.anchor.core.data.repository.AnchorRepository
import io.github.uveshm003.anchor.core.data.repository.ModeRepository
import io.github.uveshm003.anchor.core.data.repository.SettingsRepository
import io.github.uveshm003.anchor.core.engine.Environment
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Samples the [Environment] once per input, at dequeue (ADR 0003 §3). */
class EnvironmentSampler @Inject constructor(
    private val clock: Clock,
    private val modes: ModeRepository,
    private val anchors: AnchorRepository,
    private val settings: SettingsRepository,
    private val essentialApps: EssentialApps,
    private val uptime: DeviceUptime,
) {
    /**
     * Emits after each change to the saved modes (not for the list at subscription), so schedules and trigger
     * registrations follow edits, deletions and reordering without waiting for the next app start.
     */
    val modeChanges: Flow<Unit> = modes.modes.distinctUntilChanged().drop(1).map { }

    suspend fun sample(): Environment {
        val user = settings.settings.first()
        return Environment(
            now = clock.instant(),
            modes = modes.getModes(),
            anchors = anchors.getAnchors(),
            defaultModeId = user.defaultModeId,
            essentials = essentialApps.packages(includeDefaultSms = user.allowDefaultSmsApp),
            emergencyQuotaPerWeek = user.emergencyQuotaPerWeek,
            elapsedRealtimeMillis = uptime.elapsedRealtimeMillis(),
            bootCount = uptime.bootCount(),
            zone = clock.zone,
        )
    }
}
