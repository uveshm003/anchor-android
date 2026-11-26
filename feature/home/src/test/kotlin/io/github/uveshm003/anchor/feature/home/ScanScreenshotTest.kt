package io.github.uveshm003.anchor.feature.home

import io.github.uveshm003.anchor.core.designsystem.theme.AnchorTheme
import io.github.uveshm003.anchor.core.screenshottesting.ScreenshotRule
import io.github.uveshm003.anchor.core.screenshottesting.ScreenshotVariant
import io.github.uveshm003.anchor.feature.home.scan.ScanScreen
import io.github.uveshm003.anchor.feature.home.scan.ScanState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

/** The live camera preview can't render here; its states around it are captured. */
@RunWith(ParameterizedRobolectricTestRunner::class)
class ScanScreenshotTest(variant: ScreenshotVariant) {
    @get:Rule
    val screenshot = ScreenshotRule(variant)

    private fun capture(name: String, state: ScanState, granted: Boolean = true) = screenshot.capture(name) { dark ->
        AnchorTheme(darkTheme = dark, dynamicColor = false) { ScanScreen(state, granted, {}, {}, {}) }
    }

    @Test
    fun noPermission() = capture("scan_permission", ScanState.Scanning, granted = false)

    @Test
    fun ended() = capture("scan_ended", ScanState.Ended)

    @Test
    fun wrongAnchor() = capture("scan_wrong_anchor", ScanState.WrongAnchor("Bedroom"))

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants() = ScreenshotVariant.parameters()
    }
}
