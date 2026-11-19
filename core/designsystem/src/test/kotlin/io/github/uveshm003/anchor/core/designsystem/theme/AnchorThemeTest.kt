package io.github.uveshm003.anchor.core.designsystem.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** `ColorScheme` has no `equals()`, so schemes are compared by identity or by individual colour values. */
@RunWith(AndroidJUnit4::class)
class AnchorThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun brandLightScheme_whenDynamicColorOff() {
        assertSame(
            BrandLightColorScheme,
            capture {
                AnchorTheme(darkTheme = false, dynamicColor = false, content = it)
            }.colors,
        )
    }

    @Test
    fun brandDarkScheme_whenDynamicColorOffAndDark() {
        assertSame(
            BrandDarkColorScheme,
            capture {
                AnchorTheme(darkTheme = true, dynamicColor = false, content = it)
            }.colors,
        )
    }

    @Test
    fun dynamicColor_isOnByDefault_light() {
        val expected = dynamicLightColorScheme(context)
        // Guard: the system palette must differ from the brand one, or this test could not tell them apart.
        assertNotEquals(BrandLightColorScheme.primary, expected.primary)

        val actual = capture { AnchorTheme(darkTheme = false, content = it) }.colors

        assertEquals(expected.primary, actual.primary)
        assertEquals(expected.surface, actual.surface)
        assertEquals(expected.onSurface, actual.onSurface)
    }

    @Test
    fun dynamicColor_isOnByDefault_dark() {
        val expected = dynamicDarkColorScheme(context)
        assertNotEquals(BrandDarkColorScheme.primary, expected.primary)

        val actual = capture { AnchorTheme(darkTheme = true, content = it) }.colors

        assertEquals(expected.primary, actual.primary)
        assertEquals(expected.surface, actual.surface)
        assertEquals(expected.onSurface, actual.onSurface)
    }

    @Test
    fun lightSchemes_areLighterThanDarkSchemes() {
        for (dynamic in listOf(false, true)) {
            val light = anchorColorScheme(darkTheme = false, dynamicColor = dynamic, context = context)
            val dark = anchorColorScheme(darkTheme = true, dynamicColor = dynamic, context = context)
            assertTrue("dynamic=$dynamic surface", light.surface.luminance() > dark.surface.luminance())
            assertTrue("dynamic=$dynamic onSurface", light.onSurface.luminance() < dark.onSurface.luminance())
        }
    }

    @Test
    fun typographyAndShapes_comeFromDesignSystem() {
        val captured = capture { AnchorTheme(dynamicColor = false, content = it) }
        assertSame(AnchorTypography, captured.typography)
        assertSame(AnchorShapes, captured.shapes)
    }

    private data class Captured(val colors: ColorScheme, val typography: Typography, val shapes: Shapes)

    private fun capture(
        theme: @androidx.compose.runtime.Composable (@androidx.compose.runtime.Composable () -> Unit) -> Unit,
    ): Captured {
        lateinit var captured: Captured
        composeRule.setContent {
            theme {
                captured = Captured(MaterialTheme.colorScheme, MaterialTheme.typography, MaterialTheme.shapes)
            }
        }
        composeRule.waitForIdle()
        return captured
    }
}
