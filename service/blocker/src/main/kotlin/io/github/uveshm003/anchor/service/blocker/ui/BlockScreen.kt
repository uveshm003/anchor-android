package io.github.uveshm003.anchor.service.blocker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.uveshm003.anchor.core.designsystem.component.AnchorButton
import io.github.uveshm003.anchor.core.designsystem.component.AnchorIcons
import io.github.uveshm003.anchor.core.model.ExitRule
import io.github.uveshm003.anchor.service.blocker.R
import io.github.uveshm003.anchor.service.blocker.policy.ActivePolicy
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Read top to bottom by TalkBack: what's on, for how long, how it ends, then the actions. */
@Composable
fun BlockScreen(
    policy: ActivePolicy,
    now: Instant,
    zone: ZoneId,
    onGoHome: () -> Unit,
    onEmergencyUnlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box(Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    painterResource(AnchorIcons.Anchor),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp),
                )
                Text(
                    text = stringResource(R.string.service_blocker_title, policy.modeName),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                val minutes = Duration.between(policy.startedAt, now).toMinutes().coerceAtLeast(0).toInt()
                Text(
                    text = pluralStringResource(R.plurals.service_blocker_elapsed, minutes, minutes),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = howItEnds(policy, zone),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                AnchorButton(text = stringResource(R.string.service_blocker_go_home), onClick = onGoHome)
                TextButton(onClick = onEmergencyUnlock) { Text(stringResource(R.string.service_blocker_emergency)) }
            }
        }
    }
}

@Composable
private fun howItEnds(policy: ActivePolicy, zone: ZoneId): String = when (policy.exitRule) {
    is ExitRule.RequiresAnchor ->
        policy.requiredAnchorName
            ?.let { stringResource(R.string.service_blocker_ends_specific_anchor, it) }
            ?: stringResource(R.string.service_blocker_ends_any_anchor)

    is ExitRule.TimerEnds -> stringResource(
        R.string.service_blocker_ends_at,
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(checkNotNull(policy.endsAt).atZone(zone)),
    )

    ExitRule.TriggerEnds -> stringResource(R.string.service_blocker_ends_trigger)
}
