package io.github.uveshm003.anchor.feature.modes.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.uveshm003.anchor.core.designsystem.component.AnchorIconButton
import io.github.uveshm003.anchor.core.designsystem.component.AnchorIcons
import io.github.uveshm003.anchor.core.designsystem.component.AnchorListRow
import io.github.uveshm003.anchor.core.designsystem.component.AnchorTopAppBar
import io.github.uveshm003.anchor.core.designsystem.component.ListRowTrailing
import io.github.uveshm003.anchor.core.designsystem.component.SectionHeader
import io.github.uveshm003.anchor.core.model.Anchor
import io.github.uveshm003.anchor.core.model.ModeColor
import io.github.uveshm003.anchor.core.model.ModeIcon
import io.github.uveshm003.anchor.core.ui.color
import io.github.uveshm003.anchor.core.ui.drawable
import io.github.uveshm003.anchor.core.ui.label
import io.github.uveshm003.anchor.feature.modes.R

@Composable
fun ModeEditorRoute(onDone: () -> Unit, viewModel: ModeEditorViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnDone by rememberUpdatedState(onDone)
    LaunchedEffect(viewModel) { viewModel.effects.collect { if (it == ModeEditorEffect.Saved) currentOnDone() } }
    if (state.pickingApps) {
        AppPickerScreen(
            state = state,
            onBack = { viewModel.showAppPicker(false) },
            onQuery = viewModel::setQuery,
            onToggle = viewModel::toggleApp,
            onSelectAllVisible = viewModel::selectAllShown,
        )
    } else {
        ModeEditorScreen(
            state = state,
            actions = ModeEditorActions(
                onClose = onDone,
                onSave = viewModel::save,
                onName = viewModel::setName,
                onIcon = { viewModel.setLook(icon = it) },
                onColor = { viewModel.setLook(color = it) },
                onAllowList = viewModel::setAllowList,
                onPickApps = { viewModel.showAppPicker(true) },
                onExit = viewModel::setExit,
                onTrigger = viewModel::setTrigger,
            ),
        )
    }
}

data class ModeEditorActions(
    val onClose: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onName: (String) -> Unit = {},
    val onIcon: (ModeIcon) -> Unit = {},
    val onColor: (ModeColor) -> Unit = {},
    val onAllowList: (Boolean) -> Unit = {},
    val onPickApps: () -> Unit = {},
    val onExit: (ExitChoice) -> Unit = {},
    val onTrigger: (TriggerChange) -> Unit = {},
)

@Composable
fun ModeEditorScreen(state: ModeEditorUiState, actions: ModeEditorActions, modifier: Modifier = Modifier) {
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val close = { if (state.dirty) confirmDiscard = true else actions.onClose() }
    BackHandler(onBack = close)
    Scaffold(
        modifier = modifier,
        topBar = {
            AnchorTopAppBar(
                title = stringResource(
                    if (state.isNew) R.string.feature_modes_editor_new else R.string.feature_modes_editor_edit,
                ),
                navigationIcon = {
                    AnchorIconButton(AnchorIcons.Close, stringResource(R.string.feature_modes_cancel), close)
                },
                actions = {
                    TextButton(onClick = actions.onSave) { Text(stringResource(R.string.feature_modes_save)) }
                },
            )
        },
    ) { padding ->
        if (!state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                EditorForm(state, actions)
            }
        }
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.feature_modes_discard_title)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    actions.onClose()
                }) { Text(stringResource(R.string.feature_modes_discard)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) {
                    Text(stringResource(R.string.feature_modes_keep_editing))
                }
            },
        )
    }
}

@Composable
private fun EditorForm(state: ModeEditorUiState, actions: ModeEditorActions) {
    Column(
        modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        LookSection(state, actions)
        AppsSection(state, actions)
        AutomaticSection(state, actions.onTrigger)
        ExitSection(state.draft, state.anchors, actions.onExit)
    }
}

/** Name, icon and colour. */
@Composable
private fun LookSection(state: ModeEditorUiState, actions: ModeEditorActions) {
    Column {
        val draft = state.draft
        // Local, synchronous text state (seeded once the draft has loaded): a field fed back through an async
        // flow drops fast keystrokes.
        var name by rememberSaveable { mutableStateOf(draft.name) }
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                actions.onName(it)
            },
            label = { Text(stringResource(R.string.feature_modes_name)) },
            isError = state.nameError,
            supportingText = if (state.nameError) {
                { Text(stringResource(R.string.feature_modes_name_error)) }
            } else {
                null
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )
        SectionHeader(stringResource(R.string.feature_modes_icon))
        ChipRow {
            ModeIcon.entries.forEach { icon ->
                FilterChip(
                    selected = icon == draft.icon,
                    onClick = { actions.onIcon(icon) },
                    label = { Text(stringResource(icon.label)) },
                    leadingIcon = {
                        Icon(painterResource(icon.drawable), contentDescription = null, Modifier.size(18.dp))
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        SectionHeader(stringResource(R.string.feature_modes_color))
        ChipRow {
            ModeColor.entries.forEach { color ->
                ColorSwatch(color, selected = color == draft.color) { actions.onColor(color) }
            }
        }
    }
}

/** Block or allow list, and the apps. */
@Composable
private fun AppsSection(state: ModeEditorUiState, actions: ModeEditorActions) {
    Column {
        val draft = state.draft
        SectionHeader(stringResource(R.string.feature_modes_policy))
        Column(Modifier.selectableGroup()) {
            Choice(
                stringResource(R.string.feature_modes_block_list),
                stringResource(R.string.feature_modes_block_list_body),
                !draft.allowList,
            ) { actions.onAllowList(false) }
            Choice(
                stringResource(R.string.feature_modes_allow_list),
                stringResource(R.string.feature_modes_allow_list_body),
                draft.allowList,
            ) { actions.onAllowList(true) }
        }
        val count = (draft.packages - state.essentials).size
        AnchorListRow(
            title = stringResource(
                if (draft.allowList) R.string.feature_modes_apps_allowed else R.string.feature_modes_apps_blocked,
            ),
            supportingText = pluralStringResource(R.plurals.feature_modes_apps_selected, count, count),
            leadingIcon = AnchorIcons.Block,
            trailing = ListRowTrailing.Chevron(actions.onPickApps),
        )
    }
}

/** Anchor tap or a timer. */
@Composable
private fun ExitSection(draft: ModeDraft, anchors: List<Anchor>, onExit: (ExitChoice) -> Unit) {
    val timer = draft.timerMinutes
    val specific = draft.anchorId?.takeIf { id -> anchors.any { it.id.value == id } }
    Column {
        SectionHeader(stringResource(R.string.feature_modes_exit))
        Column(Modifier.selectableGroup()) {
            Choice(
                stringResource(R.string.feature_modes_exit_anchor),
                stringResource(R.string.feature_modes_exit_anchor_body),
                timer == null && specific == null && !draft.endsWithTrigger,
            ) { onExit(ExitChoice.AnyAnchor) }
            if (anchors.isNotEmpty()) {
                Choice(
                    stringResource(R.string.feature_modes_exit_specific),
                    stringResource(R.string.feature_modes_exit_specific_body),
                    timer == null && specific != null,
                ) { onExit(ExitChoice.SpecificAnchor(specific ?: anchors.first().id.value)) }
            }
            if (draft.automatic) TriggerExitChoice(draft) { onExit(ExitChoice.TriggerEnds) }
        }
        if (timer == null && specific != null) {
            ChipRow {
                anchors.forEach { anchor ->
                    FilterChip(
                        selected = anchor.id.value == specific,
                        onClick = { onExit(ExitChoice.SpecificAnchor(anchor.id.value)) },
                        label = { Text(anchor.name) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
        }
        Column(Modifier.selectableGroup()) {
            Choice(
                stringResource(R.string.feature_modes_exit_timer),
                stringResource(R.string.feature_modes_exit_timer_body),
                timer != null,
            ) { onExit(ExitChoice.Timer(timer ?: DEFAULT_TIMER)) }
        }
        if (timer != null) {
            ChipRow {
                TIMER_CHOICES.forEach { minutes ->
                    FilterChip(
                        selected = minutes == timer,
                        onClick = { onExit(ExitChoice.Timer(minutes)) },
                        label = { Text(stringResource(R.string.feature_modes_minutes, minutes)) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    FlowRow(
        modifier = Modifier.padding(horizontal = 16.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) { content() }
}

/** "Ends when its trigger ends", worded for the mode's automatic triggers. */
@Composable
private fun TriggerExitChoice(draft: ModeDraft, onSelect: () -> Unit) {
    val (title, body) = when {
        draft.schedule == null -> R.string.feature_modes_exit_any_trigger to R.string.feature_modes_exit_usb_body
        !draft.usb -> R.string.feature_modes_exit_trigger to R.string.feature_modes_exit_trigger_body
        else -> R.string.feature_modes_exit_any_trigger to R.string.feature_modes_exit_any_trigger_body
    }
    Choice(stringResource(title), stringResource(body), draft.endsWithTrigger, onSelect)
}

@Composable
private fun Choice(title: String, body: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).selectable(selected, onClick = onSelect, role = Role.RadioButton)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ColorSwatch(color: ModeColor, selected: Boolean, onSelect: () -> Unit) {
    val name = stringResource(color.label)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .border(if (selected) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
            .padding(6.dp)
            .clip(CircleShape)
            .background(color.color)
            .selectable(selected, onClick = onSelect, role = Role.RadioButton)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(painterResource(AnchorIcons.Check), contentDescription = null, tint = Color.White)
    }
}

private const val DEFAULT_TIMER = 25
private val TIMER_CHOICES = listOf(15, 25, 45, 60, 90, 120)
