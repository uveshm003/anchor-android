package io.github.uveshm003.anchor.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/** Primary action: a filled button at least 48 dp tall, with an optional leading icon (decorative). */
@Composable
fun AnchorButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes leadingIcon: Int? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = MIN_TOUCH),
        enabled = enabled,
        contentPadding = contentPadding(leadingIcon),
    ) {
        ButtonContent(text, leadingIcon)
    }
}

/** Secondary action: an outlined button at least 48 dp tall, with an optional leading icon (decorative). */
@Composable
fun AnchorOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes leadingIcon: Int? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = MIN_TOUCH),
        enabled = enabled,
        contentPadding = contentPadding(leadingIcon),
    ) {
        ButtonContent(text, leadingIcon)
    }
}

@Composable
private fun RowScope.ButtonContent(text: String, @DrawableRes leadingIcon: Int?) {
    if (leadingIcon != null) {
        // The label already says what the button does, so the icon is decorative.
        Icon(painterResource(leadingIcon), contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
    }
    Text(text)
}

/** Material 3 uses tighter start padding when a button has a leading icon. */
private fun contentPadding(@DrawableRes leadingIcon: Int?): PaddingValues =
    if (leadingIcon != null) ButtonDefaults.ButtonWithIconContentPadding else ButtonDefaults.ContentPadding

private val MIN_TOUCH = 48.dp
