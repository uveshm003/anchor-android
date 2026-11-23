package io.github.uveshm003.anchor.service.blocker

import io.github.uveshm003.anchor.core.designsystem.theme.AnchorTheme
import io.github.uveshm003.anchor.core.model.AppPolicy
import io.github.uveshm003.anchor.core.model.ExitRule
import io.github.uveshm003.anchor.core.model.SessionId
import io.github.uveshm003.anchor.core.screenshottesting.ScreenshotRule
import io.github.uveshm003.anchor.core.screenshottesting.ScreenshotVariant
import io.github.uveshm003.anchor.service.blocker.policy.ActivePolicy
import io.github.uveshm003.anchor.service.blocker.ui.BlockScreen
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

@RunWith(ParameterizedRobolectricTestRunner::class)
class BlockScreenScreenshotTest(variant: ScreenshotVariant) {
    @get:Rule
    val screenshot = ScreenshotRule(variant)

    private val start = Instant.parse("2026-11-23T10:00:00Z")

    private fun policy(rule: ExitRule, endsAt: Instant? = null, anchor: String? = null) =
        ActivePolicy(SessionId(1), "Deep Work", AppPolicy.BlockList(setOf("com.social")), start, rule, endsAt, anchor)

    private fun capture(name: String, policy: ActivePolicy) = screenshot.capture(name) { dark ->
        AnchorTheme(darkTheme = dark, dynamicColor = false) {
            BlockScreen(policy, now = start.plusSeconds(25 * 60), onGoHome = {
            }, onEmergencyUnlock = {}, zone = ZoneOffset.UTC)
        }
    }

    @Test
    fun anyAnchor() = capture("block_screen_any_anchor", policy(ExitRule.RequiresAnchor()))

    @Test
    fun specificAnchor() = capture("block_screen_specific_anchor", policy(ExitRule.RequiresAnchor(), anchor = "Desk"))

    @Test
    fun timer() = capture("block_screen_timer", policy(ExitRule.TimerEnds(45), endsAt = start.plusSeconds(45 * 60)))

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants() = ScreenshotVariant.parameters()
    }
}
