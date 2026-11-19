package io.github.uveshm003.anchor.core.screenshottesting

import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

/** Each variant reaches the composition, and a class-level locale survives the variant qualifiers. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(qualifiers = "hi")
class ScreenshotRuleTest(private val variant: ScreenshotVariant) {
    @get:Rule
    val screenshot = ScreenshotRule(variant)

    @Test
    fun configurationMatchesTheVariant() {
        lateinit var configuration: Configuration
        var darkThemeArgument: Boolean? = null
        screenshot.render { dark ->
            darkThemeArgument = dark
            configuration = LocalConfiguration.current
        }

        val night = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        assertEquals(variant.darkTheme, darkThemeArgument)
        assertEquals(if (variant.darkTheme) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO, night)
        assertEquals("hi", configuration.locales[0].language)
        when (variant.device) {
            ScreenshotDevice.Phone -> {
                assertEquals(411, configuration.screenWidthDp)
                assertEquals(480, configuration.densityDpi)
                assertEquals(Configuration.ORIENTATION_PORTRAIT, configuration.orientation)
            }

            ScreenshotDevice.Tablet -> {
                assertEquals(1280, configuration.screenWidthDp)
                assertEquals(320, configuration.densityDpi)
                assertEquals(Configuration.ORIENTATION_LANDSCAPE, configuration.orientation)
            }
        }
    }

    @Test
    fun variantNames_areStableGoldenSuffixes() {
        assertEquals(
            listOf("phone_light", "phone_dark", "tablet_light", "tablet_dark"),
            ScreenshotVariant.all().map(ScreenshotVariant::toString),
        )
        assertEquals(
            listOf("phone_light", "phone_dark"),
            ScreenshotVariant.all(listOf(ScreenshotDevice.Phone)).map {
                "$it"
            },
        )
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants() = ScreenshotVariant.parameters()
    }
}
