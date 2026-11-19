package io.github.uveshm003.anchor.core.designsystem.theme

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.uveshm003.anchor.core.testing.screenshot.ScreenshotDevice
import io.github.uveshm003.anchor.core.testing.screenshot.captureThemedScreenshots
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every colour role that Material components draw with, and every type style, so palette or typography
 * changes show up in review. The 12 `*Fixed*` roles are not set by the brand palette yet (#85) and are
 * not shown. The `dynamic_*` goldens pin the dynamic-colour wiring, rendered with Robolectric's built-in
 * system colours, not a real wallpaper palette.
 */
@RunWith(AndroidJUnit4::class)
class ThemeSpecimenScreenshotTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun colors() = composeRule.captureThemedScreenshots("theme_colors") { darkTheme ->
        Specimen(darkTheme) { ColorSpecimen() }
    }

    @Test
    fun type() = composeRule.captureThemedScreenshots("theme_type") { darkTheme ->
        Specimen(darkTheme) { TypeSpecimen() }
    }

    @Test
    fun dynamicColors() = composeRule.captureThemedScreenshots(
        name = "theme_dynamic_colors",
        devices = listOf(ScreenshotDevice.Phone),
    ) { darkTheme ->
        Specimen(darkTheme, dynamicColor = true) { ColorSpecimen() }
    }
}

@Composable
private fun Specimen(darkTheme: Boolean, dynamicColor: Boolean = false, content: @Composable () -> Unit) {
    AnchorTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
        Surface(content = content)
    }
}

private data class Role(val name: String, val background: Color, val content: Color, val border: Color? = null)

@Composable
private fun ColorSpecimen() {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        colorRoles(MaterialTheme.colorScheme).forEach { role ->
            Box(
                modifier = Modifier
                    .size(width = 120.dp, height = 40.dp)
                    .background(role.background, MaterialTheme.shapes.small)
                    .then(role.border?.let { Modifier.border(2.dp, it, MaterialTheme.shapes.small) } ?: Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Text(role.name, color = role.content, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun colorRoles(c: ColorScheme) = listOf(
    Role("primary", c.primary, c.onPrimary),
    Role("primaryContainer", c.primaryContainer, c.onPrimaryContainer),
    Role("inversePrimary", c.inverseSurface, c.inversePrimary),
    Role("secondary", c.secondary, c.onSecondary),
    Role("secondaryContainer", c.secondaryContainer, c.onSecondaryContainer),
    Role("tertiary", c.tertiary, c.onTertiary),
    Role("tertiaryContainer", c.tertiaryContainer, c.onTertiaryContainer),
    Role("error", c.error, c.onError),
    Role("errorContainer", c.errorContainer, c.onErrorContainer),
    Role("background", c.background, c.onBackground),
    Role("surface", c.surface, c.onSurface),
    Role("surfaceVariant", c.surfaceVariant, c.onSurfaceVariant),
    Role("surfaceDim", c.surfaceDim, c.onSurface),
    Role("surfaceBright", c.surfaceBright, c.onSurface),
    Role("containerLowest", c.surfaceContainerLowest, c.onSurface),
    Role("containerLow", c.surfaceContainerLow, c.onSurface),
    Role("container", c.surfaceContainer, c.onSurface),
    Role("containerHigh", c.surfaceContainerHigh, c.onSurface),
    Role("containerHighest", c.surfaceContainerHighest, c.onSurface),
    Role("inverseSurface", c.inverseSurface, c.inverseOnSurface),
    Role("outline", c.surface, c.onSurface, border = c.outline),
    Role("outlineVariant", c.surface, c.onSurface, border = c.outlineVariant),
    Role("scrim", c.scrim, Color.White),
)

@Composable
private fun TypeSpecimen() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        typeStyles(MaterialTheme.typography).forEach { (name, style) -> Text(name, style = style) }
    }
}

private fun typeStyles(t: Typography) = listOf(
    "Display large" to t.displayLarge,
    "Display medium" to t.displayMedium,
    "Display small" to t.displaySmall,
    "Headline large" to t.headlineLarge,
    "Headline medium" to t.headlineMedium,
    "Headline small" to t.headlineSmall,
    "Title large" to t.titleLarge,
    "Title medium" to t.titleMedium,
    "Title small" to t.titleSmall,
    "Body large" to t.bodyLarge,
    "Body medium" to t.bodyMedium,
    "Body small" to t.bodySmall,
    "Label large" to t.labelLarge,
    "Label medium" to t.labelMedium,
    "Label small" to t.labelSmall,
)
