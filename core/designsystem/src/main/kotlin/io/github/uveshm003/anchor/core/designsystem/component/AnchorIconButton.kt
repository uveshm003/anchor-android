package io.github.uveshm003.anchor.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource

/**
 * Icon-only button (48 dp). [contentDescription] is required: an icon-only control must say what it does
 * (e.g. "Back", "Delete mode").
 */
@Composable
fun AnchorIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    IconButton(onClick = onClick, modifier = modifier, enabled = enabled) {
        Icon(painterResource(icon), contentDescription = contentDescription)
    }
}
