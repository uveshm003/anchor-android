package io.github.uveshm003.anchor.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnchorThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun brandLightScheme_whenDynamicColorOff() {
        assertEquals(BrandLightColorScheme, capture(darkTheme = false).colors)
    }

    @Test
    fun brandDarkScheme_whenDynamicColorOffAndDark() {
        assertEquals(BrandDarkColorScheme, capture(darkTheme = true).colors)
    }

    @Test
    fun brandSchemes_differBetweenLightAndDark() {
        assertNotEquals(BrandLightColorScheme.surface, BrandDarkColorScheme.surface)
        assertNotEquals(BrandLightColorScheme.onSurface, BrandDarkColorScheme.onSurface)
    }

    @Test
    fun dynamicColor_usesSystemPalette() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dynamicLight = anchorColorScheme(darkTheme = false, dynamicColor = true, context = context)
        val dynamicDark = anchorColorScheme(darkTheme = true, dynamicColor = true, context = context)
        assertNotEquals(BrandLightColorScheme, dynamicLight)
        assertNotEquals(dynamicLight.surface, dynamicDark.surface)
    }

    @Test
    fun typographyAndShapes_comeFromDesignSystem() {
        val captured = capture(darkTheme = false)
        assertEquals(AnchorTypography, captured.typography)
        assertEquals(AnchorShapes, captured.shapes)
    }

    private data class Captured(val colors: ColorScheme, val typography: Typography, val shapes: Shapes)

    private fun capture(darkTheme: Boolean): Captured {
        lateinit var captured: Captured
        composeRule.setContent {
            AnchorTheme(darkTheme = darkTheme, dynamicColor = false) {
                captured = Captured(MaterialTheme.colorScheme, MaterialTheme.typography, MaterialTheme.shapes)
            }
        }
        composeRule.waitForIdle()
        return captured
    }
}
