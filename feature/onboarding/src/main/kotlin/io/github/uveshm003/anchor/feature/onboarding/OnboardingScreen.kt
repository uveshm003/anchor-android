package io.github.uveshm003.anchor.feature.onboarding

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.uveshm003.anchor.core.designsystem.component.AnchorButton
import io.github.uveshm003.anchor.core.designsystem.component.AnchorIcons

@Composable
fun OnboardingRoute(viewModel: OnboardingViewModel = hiltViewModel()) {
    val step by viewModel.step.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collect {
            context.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
    OnboardingScreen(
        step = step,
        actions = OnboardingActions(viewModel::next, viewModel::agree, viewModel::openSettings, viewModel::notNow),
    )
}

data class OnboardingActions(
    val onNext: () -> Unit = {},
    val onAgree: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onNotNow: () -> Unit = {},
)

@Composable
fun OnboardingScreen(step: OnboardingStep, actions: OnboardingActions, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxSize()) {
        Box(
            Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (step) {
                    OnboardingStep.Welcome -> Page(
                        icon = AnchorIcons.Anchor,
                        title = stringResource(R.string.feature_onboarding_welcome_title),
                        body = stringResource(R.string.feature_onboarding_welcome_body),
                    ) {
                        AnchorButton(
                            text = stringResource(R.string.feature_onboarding_get_started),
                            onClick = actions.onNext,
                        )
                    }

                    OnboardingStep.Disclosure -> Page(
                        icon = AnchorIcons.Info,
                        title = stringResource(R.string.feature_onboarding_disclosure_title),
                        body = stringResource(R.string.feature_onboarding_disclosure_body),
                    ) {
                        AnchorButton(
                            text = stringResource(R.string.feature_onboarding_agree),
                            onClick = actions.onAgree,
                        )
                        TextButton(onClick = actions.onNotNow) {
                            Text(stringResource(R.string.feature_onboarding_not_now))
                        }
                    }

                    OnboardingStep.Enable -> Page(
                        icon = AnchorIcons.Settings,
                        title = stringResource(R.string.feature_onboarding_enable_title),
                        body = stringResource(R.string.feature_onboarding_enable_body),
                    ) {
                        AnchorButton(
                            text = stringResource(R.string.feature_onboarding_open_settings),
                            onClick = actions.onOpenSettings,
                        )
                        TextButton(onClick = actions.onNotNow) {
                            Text(stringResource(R.string.feature_onboarding_skip))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Page(icon: Int, title: String, body: String, buttons: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(
            painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp),
        )
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        Text(body, style = MaterialTheme.typography.bodyLarge)
        buttons()
    }
}
