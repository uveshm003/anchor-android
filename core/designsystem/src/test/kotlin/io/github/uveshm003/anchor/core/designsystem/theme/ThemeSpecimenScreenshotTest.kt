package io.github.uveshm003.anchor.core.designsystem.theme

import io.github.uveshm003.anchor.core.screenshottesting.ScreenshotDevice
import io.github.uveshm003.anchor.core.screenshottesting.ScreenshotRule
import io.github.uveshm003.anchor.core.screenshottesting.ScreenshotVariant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

/**
 * Every colour role that Material components draw with, and every type style, so palette or typography
 * changes show up in review. The 12 `*Fixed*` roles are not set by the brand palette yet (#85) and are
 * not shown.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
class ThemeSpecimenScreenshotTest(variant: ScreenshotVariant) {
    @get:Rule
    val screenshot = ScreenshotRule(variant)

    @Test
    fun colors() = screenshot.capture("theme_colors") { darkTheme -> Specimen(darkTheme) { ColorSpecimen() } }

    @Test
    fun type() = screenshot.capture("theme_type") { darkTheme -> Specimen(darkTheme) { TypeSpecimen() } }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants() = ScreenshotVariant.parameters()
    }
}

/**
 * Pins the dynamic-colour wiring, rendered with Robolectric's built-in system colours (not a real wallpaper
 * palette). Phone only: the palette doesn't depend on the device size.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
class DynamicColorSpecimenScreenshotTest(variant: ScreenshotVariant) {
    @get:Rule
    val screenshot = ScreenshotRule(variant)

    @Test
    fun dynamicColors() = screenshot.capture("theme_dynamic_colors") { darkTheme ->
        Specimen(darkTheme, dynamicColor = true) { ColorSpecimen() }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants() = ScreenshotVariant.parameters(listOf(ScreenshotDevice.Phone))
    }
}
