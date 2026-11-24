package io.github.uveshm003.anchor.feature.modes.templates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.uveshm003.anchor.core.designsystem.component.AnchorIcons
import io.github.uveshm003.anchor.core.designsystem.component.ModeBadge
import io.github.uveshm003.anchor.core.ui.color
import io.github.uveshm003.anchor.core.ui.drawable
import io.github.uveshm003.anchor.feature.modes.R

/** Pick a template (or a blank mode); [onPick] gets null for "Start from scratch". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateChooserSheet(onPick: (ModeTemplate?) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) { TemplateChooser(onPick) }
}

@Composable
fun TemplateChooser(onPick: (ModeTemplate?) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        Text(
            stringResource(R.string.feature_modes_template_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).semantics { heading() },
        )
        Choice(
            icon = { Icon(painterResource(AnchorIcons.Add), contentDescription = null, Modifier.padding(8.dp)) },
            title = stringResource(R.string.feature_modes_template_blank),
            body = stringResource(R.string.feature_modes_template_blank_body),
        ) { onPick(null) }
        ModeTemplate.entries.forEach { template ->
            Choice(
                icon = {
                    ModeBadge(
                        icon = template.look.icon.drawable,
                        color = template.look.color.color,
                        contentDescription = null,
                    )
                },
                title = stringResource(template.title),
                body = stringResource(template.description),
            ) { onPick(template) }
        }
    }
}

@Composable
private fun Choice(icon: @Composable () -> Unit, title: String, body: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(
            min = 72.dp,
        ).clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        icon()
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
