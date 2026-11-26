package io.github.uveshm003.anchor.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.uveshm003.anchor.core.designsystem.component.AnchorIcons
import io.github.uveshm003.anchor.core.designsystem.component.AnchorTopAppBar
import io.github.uveshm003.anchor.core.designsystem.component.SectionHeader
import io.github.uveshm003.anchor.core.nfc.NfcAvailability

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    SettingsScreen(state = state, open = { context.open(it) })
}

/** Where a setup check sends the user. */
enum class SetupLink { Accessibility, AppDetails, BatteryOptimization, DontKillMyApp, NfcSettings }

@Composable
fun SettingsScreen(state: SetupState, open: (SetupLink) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { AnchorTopAppBar(title = stringResource(R.string.feature_settings_title)) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SectionHeader(stringResource(R.string.feature_settings_setup))
                BlockingCheck(state, open)
                BatteryCheck(state, open)
                NfcCheck(state.nfc, open)
            }
        }
    }
}

@Composable
private fun BlockingCheck(state: SetupState, open: (SetupLink) -> Unit) {
    val title = R.string.feature_settings_blocking
    if (state.blockerEnabled) {
        SetupCard(ok = true, title = title, body = R.string.feature_settings_blocking_on)
        return
    }
    SetupCard(ok = false, title = title, body = R.string.feature_settings_blocking_off) {
        if (state.needsRestrictedSettings) {
            Text(stringResource(R.string.feature_settings_restricted), style = MaterialTheme.typography.bodyMedium)
            LinkButton(R.string.feature_settings_open_app_info) { open(SetupLink.AppDetails) }
        }
        LinkButton(R.string.feature_settings_open_accessibility) { open(SetupLink.Accessibility) }
    }
}

@Composable
private fun BatteryCheck(state: SetupState, open: (SetupLink) -> Unit) {
    val body = when {
        state.batteryOptimized -> R.string.feature_settings_battery_optimized
        else -> R.string.feature_settings_battery_ok
    }
    SetupCard(ok = !state.batteryOptimized, title = R.string.feature_settings_battery, body = body) {
        if (state.batteryOptimized) {
            LinkButton(R.string.feature_settings_open_battery) { open(SetupLink.BatteryOptimization) }
        }
        state.oem?.let { oem ->
            Text(stringResource(R.string.feature_settings_oem, oem.name), style = MaterialTheme.typography.bodyMedium)
            LinkButton(R.string.feature_settings_open_dontkillmyapp) { open(SetupLink.DontKillMyApp) }
        }
    }
}

@Composable
private fun NfcCheck(nfc: NfcAvailability, open: (SetupLink) -> Unit) {
    val title = R.string.feature_settings_nfc
    when (nfc) {
        NfcAvailability.Enabled -> SetupCard(ok = true, title = title, body = R.string.feature_settings_nfc_on)

        NfcAvailability.Unavailable -> SetupCard(ok = true, title = title, body = R.string.feature_settings_nfc_none)

        NfcAvailability.Disabled -> SetupCard(ok = false, title = title, body = R.string.feature_settings_nfc_off) {
            LinkButton(R.string.feature_settings_open_nfc) { open(SetupLink.NfcSettings) }
        }
    }
}

@Composable
private fun LinkButton(@StringRes text: Int, onClick: () -> Unit) {
    TextButton(onClick = onClick) { Text(stringResource(text)) }
}

@Composable
private fun SetupCard(ok: Boolean, @StringRes title: Int, @StringRes body: Int, actions: @Composable () -> Unit = {}) {
    val colors = if (ok) {
        CardDefaults.cardColors()
    } else {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    }
    Card(colors = colors, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(painterResource(if (ok) AnchorIcons.Check else AnchorIcons.Warning), contentDescription = null)
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            }
            Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
            actions()
        }
    }
}

private fun Context.open(link: SetupLink) {
    val intent = when (link) {
        SetupLink.Accessibility -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

        SetupLink.AppDetails ->
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))

        // The system list; no REQUEST_IGNORE_BATTERY_OPTIMIZATIONS permission needed.
        SetupLink.BatteryOptimization -> Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

        // Opens the browser; Anchor itself has no internet access.
        SetupLink.DontKillMyApp -> Intent(Intent.ACTION_VIEW, Uri.parse("https://dontkillmyapp.com/"))

        SetupLink.NfcSettings -> Intent(Settings.ACTION_NFC_SETTINGS)
    }
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
