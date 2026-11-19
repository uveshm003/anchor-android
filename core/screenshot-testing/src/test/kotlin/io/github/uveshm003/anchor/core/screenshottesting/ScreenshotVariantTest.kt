package io.github.uveshm003.anchor.core.screenshottesting

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenshotVariantTest {
    @Test
    fun names_areStableGoldenSuffixes() {
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

    @Test
    fun qualifiers_areRelativeAndSetTheWholeScreen() {
        assertEquals(
            "+sw411dp-w411dp-h891dp-normal-long-port-notnight-xxhdpi",
            ScreenshotVariant(ScreenshotDevice.Phone, darkTheme = false).qualifiers,
        )
        assertEquals(
            "+sw800dp-w1280dp-h800dp-xlarge-notlong-land-night-xhdpi",
            ScreenshotVariant(ScreenshotDevice.Tablet, darkTheme = true).qualifiers,
        )
    }

    @Test
    fun parameters_wrapEachVariantForTheParameterizedRunner() {
        val parameters = ScreenshotVariant.parameters(listOf(ScreenshotDevice.Tablet))
        assertEquals(listOf("tablet_light", "tablet_dark"), parameters.map { it.single().toString() })
    }
}
