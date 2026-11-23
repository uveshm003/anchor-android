package io.github.uveshm003.anchor.core.screenshottesting

import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import org.junit.Assert.assertEquals

/** Renders through [rule] and checks the composition sees exactly the variant's screen and the expected locale. */
internal fun assertConfigurationMatches(rule: ScreenshotRule<*>, expectedLanguage: String) {
    lateinit var configuration: Configuration
    var darkThemeArgument: Boolean? = null
    rule.render { dark ->
        darkThemeArgument = dark
        configuration = LocalConfiguration.current
    }
    val variant = rule.variant
    val night = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    val size = configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
    val long = configuration.screenLayout and Configuration.SCREENLAYOUT_LONG_MASK

    assertEquals(variant.darkTheme, darkThemeArgument)
    assertEquals(if (variant.darkTheme) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO, night)
    assertEquals(expectedLanguage, configuration.locales[0].language)
    when (variant.device) {
        ScreenshotDevice.Phone -> {
            assertEquals(411, configuration.smallestScreenWidthDp)
            assertEquals(411, configuration.screenWidthDp)
            assertEquals(891, configuration.screenHeightDp)
            assertEquals(Configuration.SCREENLAYOUT_SIZE_NORMAL, size)
            assertEquals(Configuration.SCREENLAYOUT_LONG_YES, long)
            assertEquals(Configuration.ORIENTATION_PORTRAIT, configuration.orientation)
            assertEquals(480, configuration.densityDpi)
        }

        ScreenshotDevice.Tablet -> {
            assertEquals(800, configuration.smallestScreenWidthDp)
            assertEquals(1280, configuration.screenWidthDp)
            assertEquals(800, configuration.screenHeightDp)
            assertEquals(Configuration.SCREENLAYOUT_SIZE_XLARGE, size)
            assertEquals(Configuration.SCREENLAYOUT_LONG_NO, long)
            assertEquals(Configuration.ORIENTATION_LANDSCAPE, configuration.orientation)
            assertEquals(320, configuration.densityDpi)
        }

        ScreenshotDevice.Foldable -> {
            assertEquals(673, configuration.smallestScreenWidthDp)
            assertEquals(673, configuration.screenWidthDp)
            assertEquals(841, configuration.screenHeightDp)
            assertEquals(Configuration.SCREENLAYOUT_SIZE_LARGE, size)
            assertEquals(Configuration.SCREENLAYOUT_LONG_NO, long)
            assertEquals(Configuration.ORIENTATION_PORTRAIT, configuration.orientation)
            assertEquals(320, configuration.densityDpi)
        }
    }
}
