package io.github.uveshm003.anchor.feature.home

import androidx.compose.runtime.Composable
import io.github.uveshm003.anchor.core.designsystem.theme.AnchorTheme
import io.github.uveshm003.anchor.core.model.ExitRule
import io.github.uveshm003.anchor.core.model.Mode
import io.github.uveshm003.anchor.core.model.ModeColor
import io.github.uveshm003.anchor.core.model.ModeIcon
import io.github.uveshm003.anchor.core.model.ModeId
import io.github.uveshm003.anchor.core.model.Session
import io.github.uveshm003.anchor.core.model.SessionId
import io.github.uveshm003.anchor.core.model.SessionSource
import io.github.uveshm003.anchor.core.screenshottesting.ScreenshotRule
import io.github.uveshm003.anchor.core.screenshottesting.ScreenshotVariant
import io.github.uveshm003.anchor.feature.home.emergency.EmergencyActions
import io.github.uveshm003.anchor.feature.home.emergency.EmergencyStep
import io.github.uveshm003.anchor.feature.home.emergency.EmergencyUiState
import io.github.uveshm003.anchor.feature.home.emergency.EmergencyUnlockScreen
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

@RunWith(ParameterizedRobolectricTestRunner::class)
class HomeScreenshotTest(variant: ScreenshotVariant) {
    @get:Rule
    val screenshot = ScreenshotRule(variant)

    private val start = Instant.parse("2026-11-23T10:00:00Z")
    private val modes = listOf(
        Mode(ModeId(1), "Deep Work", ModeIcon.Focus, ModeColor.Teal),
        Mode(ModeId(2), "Sleep", ModeIcon.Sleep, ModeColor.Purple, exitRule = ExitRule.TimerEnds(45)),
    )
    private val idle =
        HomeUiState(
            loading = false,
            modes = modes,
            selectedModeId = ModeId(1),
            emergencyUnlocksLeft = 2,
            emergencyQuota = 3,
        )
    private val timerSession =
        Session(
            SessionId(1),
            ModeId(2),
            "Sleep",
            start,
            SessionSource.Hold,
            ExitRule.TimerEnds(45),
            endsAt = start.plusSeconds(45 * 60),
        )

    private fun capture(name: String, content: @Composable () -> Unit) =
        screenshot.capture(name) { dark -> AnchorTheme(darkTheme = dark, dynamicColor = false) { content() } }

    private fun home(name: String, state: HomeUiState) = capture(name) {
        HomeScreen(state, now = start.plusSeconds(25 * 60), zone = ZoneOffset.UTC, actions = HomeActions())
    }

    private fun emergency(name: String, state: EmergencyUiState) =
        capture(name) { EmergencyUnlockScreen(state, EmergencyActions()) }

    @Test
    fun idle() = home("home_idle", idle)

    @Test
    fun blockerOff() = home("home_blocker_off", idle.copy(blockerEnabled = false))

    @Test
    fun empty() = home("home_empty", HomeUiState(loading = false))

    @Test
    fun active() = home("home_active", idle.copy(active = timerSession))

    @Test
    fun activeWithRecovery() = home(
        "home_active_recovery",
        idle.copy(active = timerSession, recoveryRemaining = java.time.Duration.ofMinutes(22 * 60 + 15)),
    )

    @Test
    fun repairOffer() = home("home_repair", idle.copy(offerRepair = true))

    @Test
    fun emergencyRecoveryStarted() =
        emergency("emergency_recovery_started", EmergencyUiState(EmergencyStep.RecoveryStarted, left = 0, quota = 3))

    @Test
    fun emergencyIntro() = emergency("emergency_intro", EmergencyUiState(EmergencyStep.Intro, left = 2, quota = 3))

    @Test
    fun emergencyHold() = emergency("emergency_hold", EmergencyUiState(EmergencyStep.Hold, left = 2, quota = 3))

    @Test
    fun emergencyType() = emergency(
        "emergency_type",
        EmergencyUiState(EmergencyStep.Type, 2, 3, sentenceIndex = 0, typed = "I am choo", mismatch = true),
    )

    @Test
    fun emergencyExhausted() =
        emergency("emergency_exhausted", EmergencyUiState(EmergencyStep.Exhausted, left = 0, quota = 3))

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants() = ScreenshotVariant.parameters()
    }
}
