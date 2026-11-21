package io.github.uveshm003.anchor.core.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Settings-style row: leading icon, title, optional supporting text and a [trailing] behaviour. Interactive rows
 * are one touch target (≥ 56 dp) and one TalkBack stop. When disabled, the title, icon and switch dim, but the
 * supporting text stays readable because it usually explains why the row is off (e.g. "Needs location").
 */
@Composable
fun AnchorListRow(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    @DrawableRes leadingIcon: Int? = null,
    trailing: ListRowTrailing = ListRowTrailing.None,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val dimmed = colors.onSurface.copy(alpha = DISABLED_ALPHA)
    ListItem(
        headlineContent = { Text(title) },
        modifier = modifier
            .heightIn(min = 56.dp)
            .then(trailing.interaction(enabled))
            .semantics { if (!enabled) disabled() },
        supportingContent = supportingText?.let { { Text(it) } },
        leadingContent = leadingIcon?.let { { Icon(painterResource(it), contentDescription = null) } },
        trailingContent = when (trailing) {
            ListRowTrailing.None, is ListRowTrailing.Clickable -> null

            is ListRowTrailing.Chevron -> {
                { Icon(painterResource(AnchorIcons.ChevronRight), contentDescription = null) }
            }

            is ListRowTrailing.Switch -> {
                // The row handles the toggle; the switch is visual only so it isn't a second focus stop.
                { Switch(checked = trailing.checked, onCheckedChange = null, enabled = enabled) }
            }
        },
        colors = rowColors(enabled, dimmed),
    )
}

/** The row-level touch behaviour for each trailing type: one control, one TalkBack stop. */
private fun ListRowTrailing.interaction(enabled: Boolean): Modifier = when (this) {
    ListRowTrailing.None -> Modifier

    is ListRowTrailing.Clickable -> Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)

    is ListRowTrailing.Chevron -> Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)

    is ListRowTrailing.Switch ->
        Modifier.toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
}

/** Disabled rows dim the title and icons but keep the supporting text (the "why") readable. */
@Composable
private fun rowColors(enabled: Boolean, dimmed: Color): ListItemColors = if (enabled) {
    ListItemDefaults.colors()
} else {
    ListItemDefaults.colors(
        headlineColor = dimmed,
        supportingColor = MaterialTheme.colorScheme.onSurfaceVariant,
        leadingIconColor = dimmed,
        trailingIconColor = dimmed,
    )
}

private const val DISABLED_ALPHA = 0.38f
