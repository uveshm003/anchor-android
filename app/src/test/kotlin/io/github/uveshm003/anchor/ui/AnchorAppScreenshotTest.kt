package io.github.uveshm003.anchor.ui

import io.github.uveshm003.anchor.core.designsystem.theme.AnchorTheme
import io.github.uveshm003.anchor.core.screenshottesting.ScreenshotRule
import io.github.uveshm003.anchor.core.screenshottesting.ScreenshotVariant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

@RunWith(ParameterizedRobolectricTestRunner::class)
class AnchorAppScreenshotTest(variant: ScreenshotVariant) {
    @get:Rule
    val screenshot = ScreenshotRule(variant)

    @Test
    fun anchorApp() = screenshot.capture("anchor_app") { darkTheme ->
        AnchorTheme(darkTheme = darkTheme, dynamicColor = false) { AnchorApp() }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants() = ScreenshotVariant.parameters()
    }
}
