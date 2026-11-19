package io.github.uveshm003.anchor.core.designsystem.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/**
 * Anchor's Material 3 theme.
 *
 * Dynamic colour is on by default: every supported device (minSdk 31) has Material You palettes.
 * Pass `dynamicColor = false` for the brand palette, e.g. in previews and screenshot tests.
 */
@Composable
fun AnchorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    // ColorScheme has no equals(): without remember every recomposition would invalidate the whole tree.
    // Keyed on the configuration too, so a wallpaper/uiMode change handled in place recomputes it.
    val colorScheme = remember(darkTheme, dynamicColor, context, configuration) {
        anchorColorScheme(darkTheme, dynamicColor, context)
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AnchorTypography,
        shapes = AnchorShapes,
        content = content,
    )
}

internal fun anchorColorScheme(darkTheme: Boolean, dynamicColor: Boolean, context: Context): ColorScheme = when {
    dynamicColor && darkTheme -> dynamicDarkColorScheme(context)
    dynamicColor -> dynamicLightColorScheme(context)
    darkTheme -> BrandDarkColorScheme
    else -> BrandLightColorScheme
}
