package io.github.uveshm003.anchor.feature.modes.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.uveshm003.anchor.core.data.repository.InstalledApp
import io.github.uveshm003.anchor.core.designsystem.component.AnchorIconButton
import io.github.uveshm003.anchor.core.designsystem.component.AnchorIcons
import io.github.uveshm003.anchor.core.designsystem.component.AnchorTopAppBar
import io.github.uveshm003.anchor.core.ui.AppIcon
import io.github.uveshm003.anchor.feature.modes.R

/** Searchable app list; essentials are locked ("Always allowed"). Stable keys keep 300+ apps smooth. */
@Composable
fun AppPickerScreen(
    state: ModeEditorUiState,
    onBack: () -> Unit,
    onQuery: (String) -> Unit,
    onToggle: (String) -> Unit,
    onSelectAllVisible: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    val selected = state.draft.packages - state.essentials
    Scaffold(
        modifier = modifier,
        topBar = {
            AnchorTopAppBar(
                title = pluralStringResource(R.plurals.feature_modes_apps_selected, selected.size, selected.size),
                navigationIcon = {
                    AnchorIconButton(AnchorIcons.ArrowBack, stringResource(R.string.feature_modes_done), onBack)
                },
                actions = {
                    TextButton(onClick = onSelectAllVisible) {
                        Text(stringResource(R.string.feature_modes_select_all_shown))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQuery,
                label = { Text(stringResource(R.string.feature_modes_search_apps)) },
                leadingIcon = { Icon(painterResource(AnchorIcons.Search), contentDescription = null) },
                singleLine = true,
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyColumn(Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                items(state.shownApps, key = { it.packageName }) { app ->
                    AppRow(app, locked = app.packageName in state.essentials, checked = app.packageName in selected) {
                        onToggle(app.packageName)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRow(app: InstalledApp, locked: Boolean, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, enabled = !locked, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AppIcon(app.packageName)
        Column(Modifier.weight(1f)) {
            Text(app.label, style = MaterialTheme.typography.bodyLarge)
            if (locked) {
                Text(
                    stringResource(R.string.feature_modes_always_allowed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (locked) {
            Icon(painterResource(AnchorIcons.Lock), contentDescription = null)
        } else {
            Checkbox(checked = checked, onCheckedChange = null)
        }
    }
}
