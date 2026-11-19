package io.github.uveshm003.anchor.core.designsystem.theme

import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Colour roles and the type scale, so palette or typography changes show up in review. */
@RunWith(AndroidJUnit4::class)
class ThemeSpecimenScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w411dp-h891dp-notnight-xxhdpi")
    fun specimen_phone_light() = capture("theme_specimen_phone_light", darkTheme = false)

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun specimen_phone_dark() = capture("theme_specimen_phone_dark", darkTheme = true)

    @Test
    @Config(qualifiers = "w1280dp-h800dp-notnight-xhdpi")
    fun specimen_tablet_light() = capture("theme_specimen_tablet_light", darkTheme = false)

    @Test
    @Config(qualifiers = "w1280dp-h800dp-night-xhdpi")
    fun specimen_tablet_dark() = capture("theme_specimen_tablet_dark", darkTheme = true)

    private fun capture(name: String, darkTheme: Boolean) {
        composeRule.setContent {
            AnchorTheme(darkTheme = darkTheme, dynamicColor = false) {
                Surface { ThemeSpecimen() }
            }
        }
        // v2 test rules queue work on a StandardTestDispatcher: settle before capturing.
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }
}

@Composable
private fun ThemeSpecimen() {
    val colors = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            colorRoles(colors).forEach { (name, pair) -> Swatch(name, pair.first, pair.second) }
        }
        listOf(
            "Display small" to type.displaySmall,
            "Headline medium" to type.headlineMedium,
            "Title large" to type.titleLarge,
            "Title medium" to type.titleMedium,
            "Body large" to type.bodyLarge,
            "Body medium" to type.bodyMedium,
            "Label large" to type.labelLarge,
        ).forEach { (name, style): Pair<String, TextStyle> -> Text(name, style = style) }
    }
}

@Composable
private fun Swatch(name: String, background: Color, content: Color) {
    Box(
        modifier = Modifier.size(width = 120.dp, height = 48.dp).background(background, MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        Text(name, color = content, style = MaterialTheme.typography.labelSmall)
    }
}

private fun colorRoles(c: ColorScheme) = listOf(
    "primary" to (c.primary to c.onPrimary),
    "primaryContainer" to (c.primaryContainer to c.onPrimaryContainer),
    "secondary" to (c.secondary to c.onSecondary),
    "secondaryContainer" to (c.secondaryContainer to c.onSecondaryContainer),
    "tertiary" to (c.tertiary to c.onTertiary),
    "tertiaryContainer" to (c.tertiaryContainer to c.onTertiaryContainer),
    "error" to (c.error to c.onError),
    "errorContainer" to (c.errorContainer to c.onErrorContainer),
    "surface" to (c.surface to c.onSurface),
    "surfaceVariant" to (c.surfaceVariant to c.onSurfaceVariant),
    "surfaceContainer" to (c.surfaceContainer to c.onSurface),
    "inverseSurface" to (c.inverseSurface to c.inverseOnSurface),
)
