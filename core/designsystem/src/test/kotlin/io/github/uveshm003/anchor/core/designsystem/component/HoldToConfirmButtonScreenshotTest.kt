package io.github.uveshm003.anchor.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.uveshm003.anchor.core.designsystem.theme.AnchorTheme
import io.github.uveshm003.anchor.core.screenshottesting.ScreenshotRule
import io.github.uveshm003.anchor.core.screenshottesting.ScreenshotVariant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

/** Idle, half-held, fully held and disabled states. */
@RunWith(ParameterizedRobolectricTestRunner::class)
class HoldToConfirmButtonScreenshotTest(variant: ScreenshotVariant) {
    @get:Rule
    val screenshot = ScreenshotRule(variant)

    @Test
    fun states() = screenshot.capture("hold_to_confirm_button") { darkTheme ->
        AnchorTheme(darkTheme = darkTheme, dynamicColor = false) {
            Surface {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(0f, 0.5f, 1f).forEach { progress ->
                        HoldToConfirmButtonContent(
                            "Hold to start Deep Work",
                            { progress },
                            enabled = true,
                            Modifier.fillMaxWidth(),
                        )
                    }
                    HoldToConfirmButtonContent("Hold to start Deep Work", {
                        0f
                    }, enabled = false, Modifier.fillMaxWidth())
                }
            }
        }
    }

    /** 200 % font scale with a long Hindi label: the 56 dp minimum grows and the label wraps. */
    @Test
    fun largeFontHindi() = screenshot.capture("hold_to_confirm_button_fontscale2_hi") { darkTheme ->
        AnchorTheme(darkTheme = darkTheme, dynamicColor = false) {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)) {
                Surface {
                    HoldToConfirmButtonContent(
                        "डीप वर्क शुरू करने के लिए दबाए रखें",
                        { 0.5f },
                        enabled = true,
                        Modifier.padding(16.dp).fillMaxWidth(),
                    )
                }
            }
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants() = ScreenshotVariant.parameters()
    }
}
