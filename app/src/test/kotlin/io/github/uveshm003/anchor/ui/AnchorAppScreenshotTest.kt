package io.github.uveshm003.anchor.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import io.github.uveshm003.anchor.core.designsystem.theme.AnchorTheme
import io.github.uveshm003.anchor.core.screenshottesting.ScreenshotDevice
import io.github.uveshm003.anchor.core.screenshottesting.ScreenshotRule
import io.github.uveshm003.anchor.core.screenshottesting.ScreenshotVariant
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

/** The adaptive shell: bottom bar on the phone, rail on the unfolded foldable and the tablet. */
@RunWith(ParameterizedRobolectricTestRunner::class)
class AnchorAppScreenshotTest(private val variant: ScreenshotVariant) {
    @get:Rule
    val screenshot = ScreenshotRule(variant)

    @Test
    fun anchorApp() = screenshot.capture("anchor_app") { darkTheme ->
        AnchorTheme(darkTheme = darkTheme, dynamicColor = false) { AnchorApp(graph = { stubGraph() }) }
    }

    @Test
    fun anchorApp_fontScale2() {
        assumeTrue(variant.device == ScreenshotDevice.Phone)
        screenshot.capture("anchor_app_fontscale2") { darkTheme ->
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)) {
                AnchorTheme(darkTheme = darkTheme, dynamicColor = false) { AnchorApp(graph = { stubGraph() }) }
            }
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants() = ScreenshotVariant.parameters(ScreenshotDevice.entries)
    }
}
