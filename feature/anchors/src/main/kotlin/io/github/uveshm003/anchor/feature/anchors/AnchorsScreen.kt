package io.github.uveshm003.anchor.feature.anchors

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.uveshm003.anchor.core.designsystem.component.AnchorButton
import io.github.uveshm003.anchor.core.designsystem.component.AnchorIconButton
import io.github.uveshm003.anchor.core.designsystem.component.AnchorIcons
import io.github.uveshm003.anchor.core.designsystem.component.AnchorTopAppBar
import io.github.uveshm003.anchor.core.designsystem.component.EmptyState
import io.github.uveshm003.anchor.core.designsystem.component.EmptyStateAction
import io.github.uveshm003.anchor.core.model.Anchor
import io.github.uveshm003.anchor.core.nfc.NfcAvailability
import io.github.uveshm003.anchor.core.nfc.WriteResult

@Composable
fun AnchorsRoute(viewModel: AnchorsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshNfc()
        onPauseOrDispose { }
    }
    AnchorsScreen(
        state = state,
        actions = AnchorsActions(
            onPair = viewModel::startPairing,
            onDismissPairing = viewModel::dismissPairing,
            onSave = viewModel::save,
            onRename = viewModel::rename,
            onUnpair = viewModel::unpair,
        ),
    )
}

data class AnchorsActions(
    val onPair: () -> Unit = {},
    val onDismissPairing: () -> Unit = {},
    val onSave: (String) -> Unit = {},
    val onRename: (Anchor, String) -> Unit = { _, _ -> },
    val onUnpair: (Anchor) -> Unit = {},
)

@Composable
fun AnchorsScreen(state: AnchorsUiState, actions: AnchorsActions, modifier: Modifier = Modifier) {
    var renaming by rememberSaveable { mutableStateOf<Long?>(null) }
    val canPair = state.nfc == NfcAvailability.Enabled
    Scaffold(
        modifier = modifier,
        topBar = { AnchorTopAppBar(title = stringResource(R.string.feature_anchors_title)) },
        floatingActionButton = {
            if (canPair && state.anchors.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.feature_anchors_pair)) },
                    icon = { Icon(painterResource(AnchorIcons.Nfc), contentDescription = null) },
                    onClick = actions.onPair,
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {
            NfcNotice(state.nfc)
            if (!state.loading && state.anchors.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = AnchorIcons.Nfc,
                        title = stringResource(R.string.feature_anchors_empty_title),
                        body = stringResource(R.string.feature_anchors_empty_body),
                        action = if (canPair) {
                            EmptyStateAction(
                                stringResource(R.string.feature_anchors_pair),
                                actions.onPair,
                            )
                        } else {
                            null
                        },
                    )
                }
            } else {
                LazyColumn(Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                    items(state.anchors, key = { it.id.value }) { anchor ->
                        AnchorRow(anchor, onRename = {
                            renaming = anchor.id.value
                        }, onUnpair = { actions.onUnpair(anchor) })
                    }
                }
            }
        }
    }
    PairingDialog(state.pairing, actions)
    state.anchors.find { it.id.value == renaming }?.let { anchor ->
        NameDialog(
            title = stringResource(R.string.feature_anchors_rename),
            initial = anchor.name,
            onSave = {
                renaming = null
                actions.onRename(anchor, it)
            },
            onDismiss = { renaming = null },
        )
    }
}

@Composable
private fun NfcNotice(nfc: NfcAvailability) {
    if (nfc == NfcAvailability.Enabled) return
    val context = LocalContext.current
    Card(Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (nfc == NfcAvailability.Unavailable) {
                Text(stringResource(R.string.feature_anchors_no_nfc), style = MaterialTheme.typography.bodyLarge)
            } else {
                Text(stringResource(R.string.feature_anchors_nfc_off), style = MaterialTheme.typography.bodyLarge)
                AnchorButton(
                    text = stringResource(R.string.feature_anchors_turn_on_nfc),
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_NFC_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun AnchorRow(anchor: Anchor, onRename: () -> Unit, onUnpair: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(
            min = 64.dp,
        ).clickable(onClick = onRename).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(painterResource(AnchorIcons.Nfc), contentDescription = null)
        Column(Modifier.weight(1f)) {
            Text(anchor.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.feature_anchors_nfc_sticker),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AnchorIconButton(AnchorIcons.Delete, stringResource(R.string.feature_anchors_unpair, anchor.name), onUnpair)
    }
}

@Composable
private fun PairingDialog(pairing: Pairing, actions: AnchorsActions) {
    when (pairing) {
        Pairing.Idle -> Unit

        Pairing.Waiting -> AlertDialog(
            onDismissRequest = actions.onDismissPairing,
            icon = { Icon(painterResource(AnchorIcons.Nfc), contentDescription = null) },
            title = { Text(stringResource(R.string.feature_anchors_waiting_title)) },
            text = { Text(stringResource(R.string.feature_anchors_waiting_body)) },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = actions.onDismissPairing) { Text(stringResource(R.string.feature_anchors_cancel)) }
            },
        )

        is Pairing.Found -> NameDialog(
            title = stringResource(R.string.feature_anchors_name_title),
            initial = "",
            note = stringResource(
                if (pairing.write ==
                    WriteResult.Written
                ) {
                    R.string.feature_anchors_written
                } else {
                    R.string.feature_anchors_not_written
                },
            ),
            onSave = actions.onSave,
            onDismiss = actions.onDismissPairing,
        )

        is Pairing.AlreadyPaired -> AlertDialog(
            onDismissRequest = actions.onDismissPairing,
            title = { Text(stringResource(R.string.feature_anchors_already_paired, pairing.name)) },
            confirmButton = {
                TextButton(onClick = actions.onDismissPairing) { Text(stringResource(R.string.feature_anchors_ok)) }
            },
        )
    }
}

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    note: String? = null,
) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                note?.let { Text(it) }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.feature_anchors_name)) },
                    placeholder = { Text(stringResource(R.string.feature_anchors_name_hint)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(name)
            }, enabled = name.isNotBlank()) { Text(stringResource(R.string.feature_anchors_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_anchors_cancel)) } },
    )
}
