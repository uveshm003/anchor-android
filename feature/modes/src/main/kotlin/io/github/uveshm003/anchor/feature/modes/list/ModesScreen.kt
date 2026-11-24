package io.github.uveshm003.anchor.feature.modes.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.uveshm003.anchor.core.designsystem.component.AnchorIconButton
import io.github.uveshm003.anchor.core.designsystem.component.AnchorIcons
import io.github.uveshm003.anchor.core.designsystem.component.AnchorTopAppBar
import io.github.uveshm003.anchor.core.designsystem.component.EmptyState
import io.github.uveshm003.anchor.core.designsystem.component.EmptyStateAction
import io.github.uveshm003.anchor.core.designsystem.component.ModeBadge
import io.github.uveshm003.anchor.core.model.AppPolicy
import io.github.uveshm003.anchor.core.model.ExitRule
import io.github.uveshm003.anchor.core.model.Mode
import io.github.uveshm003.anchor.core.ui.color
import io.github.uveshm003.anchor.core.ui.drawable
import io.github.uveshm003.anchor.feature.modes.R
import io.github.uveshm003.anchor.feature.modes.templates.ModeTemplate
import io.github.uveshm003.anchor.feature.modes.templates.TemplateChooserSheet

@Composable
fun ModesRoute(
    onEditMode: (Long) -> Unit,
    onCreateMode: (ModeTemplate?, String?) -> Unit,
    viewModel: ModesViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel(),
) {
    var choosing by rememberSaveable { mutableStateOf(false) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ModesEffect.Deleted -> {
                    val result = snackbar.showSnackbar(
                        message = resources.getString(R.string.feature_modes_deleted, effect.mode.name),
                        actionLabel = resources.getString(R.string.feature_modes_undo),
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(effect.mode)
                }
            }
        }
    }
    ModesScreen(
        state = state,
        snackbar = snackbar,
        onCreate = { choosing = true },
        onEdit = { onEditMode(it.id.value) },
        onMoveUp = { viewModel.moveUp(it.id) },
        onMoveDown = { viewModel.moveDown(it.id) },
        onDelete = viewModel::delete,
    )
    if (choosing) {
        TemplateChooserSheet(
            onPick = { template ->
                choosing = false
                onCreateMode(template, template?.let { resources.getString(it.title) })
            },
            onDismiss = { choosing = false },
        )
    }
}

@Composable
fun ModesScreen(
    state: ModesUiState,
    onCreate: () -> Unit,
    onEdit: (Mode) -> Unit,
    onMoveUp: (Mode) -> Unit,
    onMoveDown: (Mode) -> Unit,
    onDelete: (Mode) -> Unit,
    modifier: Modifier = Modifier,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    var pendingDelete by rememberSaveable { mutableStateOf<Long?>(null) }
    Scaffold(
        modifier = modifier,
        topBar = { AnchorTopAppBar(title = stringResource(R.string.feature_modes_title)) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (state.modes.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.feature_modes_create)) },
                    icon = { Icon(painterResource(AnchorIcons.Add), contentDescription = null) },
                    onClick = onCreate,
                )
            }
        },
    ) { padding ->
        when {
            state.loading -> Unit

            state.modes.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = AnchorIcons.Tune,
                    title = stringResource(R.string.feature_modes_empty_title),
                    body = stringResource(R.string.feature_modes_empty_body),
                    action = EmptyStateAction(stringResource(R.string.feature_modes_create), onCreate),
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                itemsIndexed(state.modes, key = { _, mode -> mode.id.value }) { index, mode ->
                    ModeRow(
                        mode = mode,
                        canMoveUp = index > 0,
                        canMoveDown = index < state.modes.lastIndex,
                        onEdit = { onEdit(mode) },
                        onMoveUp = { onMoveUp(mode) },
                        onMoveDown = { onMoveDown(mode) },
                        onDelete = { pendingDelete = mode.id.value },
                    )
                }
            }
        }
    }
    state.modes.find { it.id.value == pendingDelete }?.let { mode ->
        DeleteDialog(mode, onDismiss = { pendingDelete = null }) {
            pendingDelete = null
            onDelete(mode)
        }
    }
}

@Composable
private fun DeleteDialog(mode: Mode, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feature_modes_delete_title, mode.name)) },
        text = { Text(stringResource(R.string.feature_modes_delete_body)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.feature_modes_delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_modes_cancel)) } },
    )
}

@Composable
private fun ModeRow(
    mode: Mode,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    val moveUp = stringResource(R.string.feature_modes_move_up, mode.name)
    val moveDown = stringResource(R.string.feature_modes_move_down, mode.name)
    Row(
        modifier = Modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(onClick = onEdit)
            .semantics {
                customActions = buildList {
                    if (canMoveUp) {
                        add(
                            CustomAccessibilityAction(moveUp) {
                                onMoveUp()
                                true
                            },
                        )
                    }
                    if (canMoveDown) {
                        add(
                            CustomAccessibilityAction(moveDown) {
                                onMoveDown()
                                true
                            },
                        )
                    }
                }
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ModeBadge(icon = mode.icon.drawable, color = mode.color.color, contentDescription = null)
        Column(Modifier.weight(1f)) {
            Text(mode.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = summary(mode),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (canMoveUp) AnchorIconButton(AnchorIcons.ArrowUpward, moveUp, onMoveUp)
        if (canMoveDown) AnchorIconButton(AnchorIcons.ArrowDownward, moveDown, onMoveDown)
        AnchorIconButton(AnchorIcons.Delete, stringResource(R.string.feature_modes_delete_named, mode.name), onDelete)
    }
}

@Composable
internal fun summary(mode: Mode): String {
    val count = mode.appPolicy.packages.size
    val apps = when (mode.appPolicy) {
        is AppPolicy.BlockList -> pluralStringResource(R.plurals.feature_modes_summary_blocks, count, count)
        is AppPolicy.AllowList -> pluralStringResource(R.plurals.feature_modes_summary_allows, count, count)
    }
    val exit = when (val rule = mode.exitRule) {
        is ExitRule.RequiresAnchor -> stringResource(R.string.feature_modes_summary_exit_anchor)
        is ExitRule.TimerEnds -> stringResource(R.string.feature_modes_summary_exit_timer, rule.minutes)
        ExitRule.TriggerEnds -> stringResource(R.string.feature_modes_summary_exit_trigger)
    }
    return stringResource(R.string.feature_modes_summary, apps, exit)
}
