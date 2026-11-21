package io.github.uveshm003.anchor.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

@RunWith(ParameterizedRobolectricTestRunner::class)
class ComponentsScreenshotTest(variant: ScreenshotVariant) {
    @get:Rule
    val screenshot = ScreenshotRule(variant)

    @Test
    fun gallery() = screenshot.capture("components_gallery") { darkTheme ->
        AnchorTheme(darkTheme = darkTheme, dynamicColor = false) { Surface { Gallery() } }
    }

    @Test
    fun galleryAt200PercentFont() = screenshot.capture("components_gallery_fontscale2") { darkTheme ->
        AnchorTheme(darkTheme = darkTheme, dynamicColor = false) {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)) {
                Surface { Gallery() }
            }
        }
    }

    @Test
    fun emptyState() = screenshot.capture("components_empty_state") { darkTheme ->
        AnchorTheme(darkTheme = darkTheme, dynamicColor = false) { Surface { SampleEmptyState() } }
    }

    @Test
    fun emptyStateAt200PercentFont() = screenshot.capture("components_empty_state_fontscale2") { darkTheme ->
        AnchorTheme(darkTheme = darkTheme, dynamicColor = false) {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)) {
                Surface { SampleEmptyState() }
            }
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants() = ScreenshotVariant.parameters()
    }
}

@Composable
private fun SampleEmptyState() {
    EmptyState(
        icon = AnchorIcons.Tune,
        title = "No modes yet",
        body = "Create a mode for each part of your day, or start from a template.",
        action = EmptyStateAction("Create mode") {},
    )
}

@Composable
private fun Gallery() {
    Column(Modifier.fillMaxWidth()) {
        AnchorTopAppBar(
            title = "Modes",
            navigationIcon = { AnchorIconButton(AnchorIcons.ArrowBack, contentDescription = "Back", onClick = {}) },
            actions = { AnchorIconButton(AnchorIcons.Search, contentDescription = "Search", onClick = {}) },
        )
        SectionHeader("Rows")
        AnchorListRow(
            title = "Hold notifications",
            supportingText = "Shown when the session ends",
            leadingIcon = AnchorIcons.NotificationsPaused,
            trailing = ListRowTrailing.Switch(checked = true) {},
        )
        AnchorListRow(title = "Anchors", leadingIcon = AnchorIcons.Nfc, trailing = ListRowTrailing.Chevron {})
        AnchorListRow(
            title = "Places",
            supportingText = "Needs location",
            leadingIcon = AnchorIcons.LocationOn,
            trailing = ListRowTrailing.Switch(checked = false) {},
            enabled = false,
        )
        SectionHeader("Buttons and badges")
        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AnchorButton("Save", onClick = {}, leadingIcon = AnchorIcons.Check)
            AnchorOutlinedButton("Cancel", onClick = {})
        }
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeBadge(AnchorIcons.Work, Color(0xFF00668B), contentDescription = "Deep Work")
            ModeBadge(AnchorIcons.School, Color(0xFFFFD54F), contentDescription = "Study")
            ModeBadge(AnchorIcons.Bedtime, Color(0xFF3949AB), contentDescription = "Sleep")
            ModeBadge(AnchorIcons.DirectionsCar, Color(0xFFE53935), contentDescription = "Driving")
            ModeBadge(AnchorIcons.FamilyRestroom, Color(0xFF43A047), contentDescription = "Family Time")
            // Translucent colour: judged as drawn over the surface (black icon on light, white on dark).
            ModeBadge(AnchorIcons.Anchor, Color(0xFF00668B).copy(alpha = 0.3f), contentDescription = "Custom")
        }
    }
}
