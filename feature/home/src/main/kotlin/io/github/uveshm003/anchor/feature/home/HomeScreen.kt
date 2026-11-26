package io.github.uveshm003.anchor.feature.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.uveshm003.anchor.core.designsystem.component.AnchorButton
import io.github.uveshm003.anchor.core.designsystem.component.AnchorIcons
import io.github.uveshm003.anchor.core.designsystem.component.AnchorOutlinedButton
import io.github.uveshm003.anchor.core.designsystem.component.AnchorTopAppBar
import io.github.uveshm003.anchor.core.designsystem.component.EmptyState
import io.github.uveshm003.anchor.core.designsystem.component.EmptyStateAction
import io.github.uveshm003.anchor.core.designsystem.component.HoldToConfirmButton
import io.github.uveshm003.anchor.core.designsystem.component.ModeBadge
import io.github.uveshm003.anchor.core.engine.RejectReason
import io.github.uveshm003.anchor.core.model.ExitRule
import io.github.uveshm003.anchor.core.model.Mode
import io.github.uveshm003.anchor.core.model.Session
import io.github.uveshm003.anchor.core.ui.color
import io.github.uveshm003.anchor.core.ui.drawable
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun HomeRoute(
    onCreateMode: () -> Unit,
    onEmergencyUnlock: () -> Unit,
    onPairAnchor: () -> Unit,
    onScanAnchor: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var explainBlocker by rememberSaveable { mutableStateOf(false) }
    val resources = LocalResources.current
    val context = LocalContext.current
    val notificationPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) viewModel.permissionGranted()
        }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                HomeEffect.BlockerOff -> explainBlocker = true
                is HomeEffect.Rejected -> snackbar.showSnackbar(resources.getString(effect.reason.message()))
            }
        }
    }
    HomeScreen(
        state = state,
        now = now,
        zone = viewModel.zone,
        snackbar = snackbar,
        actions = HomeActions(
            onSelect = viewModel::select,
            onStart = {
                viewModel.start()
                // Asked for when the user starts their first session, never at launch.
                if (needsNotificationPermission(
                        context,
                    )
                ) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onCreateMode = onCreateMode,
            onEmergencyUnlock = onEmergencyUnlock,
            onTurnOnBlocking = { explainBlocker = true },
            onCancelRecovery = viewModel::cancelRecovery,
            onPairAnchor = onPairAnchor,
            onScanAnchor = onScanAnchor,
        ),
    )
    if (explainBlocker) BlockerDisclosureDialog(onDismiss = { explainBlocker = false })
}

data class HomeActions(
    val onSelect: (io.github.uveshm003.anchor.core.model.ModeId) -> Unit = {},
    val onStart: () -> Unit = {},
    val onCreateMode: () -> Unit = {},
    val onEmergencyUnlock: () -> Unit = {},
    val onTurnOnBlocking: () -> Unit = {},
    val onCancelRecovery: () -> Unit = {},
    val onPairAnchor: () -> Unit = {},
    val onScanAnchor: () -> Unit = {},
)

@Composable
fun HomeScreen(
    state: HomeUiState,
    now: Instant,
    zone: ZoneId,
    actions: HomeActions,
    modifier: Modifier = Modifier,
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        topBar = { AnchorTopAppBar(title = stringResource(R.string.feature_home_app_title)) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            val active = state.active
            when {
                state.loading -> Unit

                active != null -> ActiveSession(active, state, now, zone, actions)

                state.modes.isEmpty() -> EmptyState(
                    icon = AnchorIcons.Anchor,
                    title = stringResource(R.string.feature_home_empty_title),
                    body = stringResource(R.string.feature_home_empty_body),
                    action = EmptyStateAction(stringResource(R.string.feature_home_create_mode), actions.onCreateMode),
                    modifier = Modifier.align(Alignment.Center),
                )

                else -> Idle(state, actions)
            }
        }
    }
}

@Composable
private fun Idle(state: HomeUiState, actions: HomeActions) {
    Column(
        Modifier.widthIn(max = 640.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (!state.blockerEnabled) BlockerOffCard(actions.onTurnOnBlocking)
        if (state.offerRepair) RepairCard(actions.onPairAnchor)
        Text(
            stringResource(R.string.feature_home_choose_mode),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        Column(Modifier.selectableGroup()) {
            state.modes.forEach { mode ->
                ModeChoice(mode, mode.id == state.selectedModeId) { actions.onSelect(mode.id) }
            }
        }
        val selected = state.modes.find { it.id == state.selectedModeId }
        HoldToConfirmButton(
            text = stringResource(R.string.feature_home_hold_to_start, selected?.name.orEmpty()),
            onConfirm = actions.onStart,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            stringResource(R.string.feature_home_hold_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.hasQrAnchors) {
            TextButton(onClick = actions.onScanAnchor) { Text(stringResource(R.string.feature_home_scan_qr)) }
        }
    }
}

@Composable
private fun ModeChoice(mode: Mode, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).selectable(selected, onClick = onSelect, role = Role.RadioButton)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        ModeBadge(icon = mode.icon.drawable, color = mode.color.color, contentDescription = null)
        Text(mode.name, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun RepairCard(onPairAnchor: () -> Unit) {
    Card {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.feature_home_repair_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.feature_home_repair_body), style = MaterialTheme.typography.bodyMedium)
            AnchorButton(text = stringResource(R.string.feature_home_repair_action), onClick = onPairAnchor)
        }
    }
}

@Composable
private fun BlockerOffCard(onTurnOn: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.feature_home_blocker_off_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.feature_home_blocker_off_body), style = MaterialTheme.typography.bodyMedium)
            AnchorButton(text = stringResource(R.string.feature_home_turn_on_blocking), onClick = onTurnOn)
        }
    }
}

@Composable
private fun ActiveSession(session: Session, state: HomeUiState, now: Instant, zone: ZoneId, actions: HomeActions) {
    val mode = state.modes.find { it.id == session.modeId }
    Column(
        Modifier.widthIn(max = 640.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        mode?.let {
            ModeBadge(icon = it.icon.drawable, color = it.color.color, contentDescription = null, size = 72.dp)
        }
        Text(
            stringResource(R.string.feature_home_active_title, session.modeName),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        val minutes = Duration.between(session.startedAt, now).toMinutes().coerceAtLeast(0).toInt()
        Text(
            pluralStringResource(R.plurals.feature_home_elapsed, minutes, minutes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(howItEnds(session, zone), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        state.recoveryRemaining?.let { remaining ->
            Text(
                stringResource(
                    R.string.feature_home_recovery_countdown,
                    remaining.toHours(),
                    remaining.toMinutesPart(),
                ),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = actions.onCancelRecovery) {
                Text(stringResource(R.string.feature_home_recovery_cancel))
            }
        }
        if (state.hasQrAnchors && session.exitRule is ExitRule.RequiresAnchor) {
            AnchorButton(text = stringResource(R.string.feature_home_scan_qr), onClick = actions.onScanAnchor)
        }
        AnchorOutlinedButton(
            text = stringResource(R.string.feature_home_emergency_unlock),
            onClick = actions.onEmergencyUnlock,
        )
        Text(
            stringResource(R.string.feature_home_emergency_left, state.emergencyUnlocksLeft, state.emergencyQuota),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun howItEnds(session: Session, zone: ZoneId): String = when (session.exitRule) {
    is ExitRule.RequiresAnchor -> stringResource(R.string.feature_home_ends_anchor)

    is ExitRule.TimerEnds -> stringResource(
        R.string.feature_home_ends_at,
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(checkNotNull(session.endsAt).atZone(zone)),
    )

    ExitRule.TriggerEnds -> stringResource(R.string.feature_home_ends_trigger)
}

/** Prominent disclosure before the user turns on the accessibility service (Play policy). */
@Composable
private fun BlockerDisclosureDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.feature_home_disclosure_title)) },
        text = { Text(stringResource(R.string.feature_home_disclosure_body)) },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }) { Text(stringResource(R.string.feature_home_disclosure_agree)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.feature_home_not_now)) } },
    )
}

private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED

private fun RejectReason.message(): Int = when (this) {
    is RejectReason.AlreadyActive -> R.string.feature_home_reject_active
    RejectReason.TriggerNotEnabled -> R.string.feature_home_reject_trigger
    else -> R.string.feature_home_reject_generic
}
