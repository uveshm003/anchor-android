package io.github.uveshm003.anchor.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A mode's icon on its colour. [contentDescription] should be the mode's name when the badge stands alone, or
 * `null` when the name is already shown next to it. The icon colour is chosen for contrast with [color].
 */
@Composable
fun ModeBadge(
    @DrawableRes icon: Int,
    color: Color,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val iconColor = if (color.luminance() > LIGHT_BACKGROUND_LUMINANCE) Color.Black else Color.White
    Box(
        modifier = modifier
            .size(size)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(size * ICON_FRACTION),
        )
    }
}

// Above this luminance black text/icons contrast better than white (≈ the 4.5:1 crossover).
private const val LIGHT_BACKGROUND_LUMINANCE = 0.179f
private const val ICON_FRACTION = 0.55f
