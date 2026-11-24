package io.github.uveshm003.anchor.core.data.session

import io.github.uveshm003.anchor.core.data.platform.DeviceUptime
import io.github.uveshm003.anchor.core.data.platform.EssentialApps
import io.github.uveshm003.anchor.core.data.repository.AnchorRepository
import io.github.uveshm003.anchor.core.data.repository.ModeRepository
import io.github.uveshm003.anchor.core.data.repository.SettingsRepository
import io.github.uveshm003.anchor.core.engine.Environment
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/** Samples the [Environment] once per input, at dequeue (ADR 0003 §3). */
class EnvironmentSampler @Inject constructor(
    private val clock: Clock,
    private val modes: ModeRepository,
    private val anchors: AnchorRepository,
    private val settings: SettingsRepository,
    private val essentialApps: EssentialApps,
    private val uptime: DeviceUptime,
) {
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
        )
    }
}
