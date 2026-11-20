package io.github.uveshm003.anchor.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Settings-style row: leading icon, title, optional supporting text and a trailing chevron or switch. The whole
 * row is the touch target (≥ 56 dp) and is announced as a single control.
 */
@Composable
fun AnchorListRow(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    @DrawableRes leadingIcon: Int? = null,
    trailing: ListRowTrailing = ListRowTrailing.None,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val interaction = when {
        trailing is ListRowTrailing.Switch ->
            Modifier.toggleable(
                value = trailing.checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = trailing.onCheckedChange,
            )

        onClick != null -> Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)

        else -> Modifier
    }
    val colors = MaterialTheme.colorScheme
    val disabled = colors.onSurface.copy(alpha = DISABLED_ALPHA)
    ListItem(
        headlineContent = { Text(title) },
        modifier = modifier
            .heightIn(min = 56.dp)
            .then(interaction)
            .semantics { if (!enabled) disabled() },
        colors = if (enabled) {
            ListItemDefaults.colors()
        } else {
            ListItemDefaults.colors(
                headlineColor = disabled,
                supportingColor = disabled,
                leadingIconColor = disabled,
                trailingIconColor = disabled,
            )
        },
        supportingContent = supportingText?.let { { Text(it) } },
        leadingContent = leadingIcon?.let { { Icon(painterResource(it), contentDescription = null) } },
        trailingContent = when (trailing) {
            ListRowTrailing.None -> null

            ListRowTrailing.Chevron -> {
                { Icon(painterResource(AnchorIcons.ChevronRight), contentDescription = null) }
            }

            is ListRowTrailing.Switch -> {
                // The row handles the toggle; the switch is visual only so it isn't a second focus stop.
                { Switch(checked = trailing.checked, onCheckedChange = null, enabled = enabled) }
            }
        },
    )
}

private const val DISABLED_ALPHA = 0.38f
