package io.github.uveshm003.anchor.feature.home.emergency

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.uveshm003.anchor.core.designsystem.component.AccessibleHold
import io.github.uveshm003.anchor.core.designsystem.component.AnchorButton
import io.github.uveshm003.anchor.core.designsystem.component.AnchorIconButton
import io.github.uveshm003.anchor.core.designsystem.component.AnchorIcons
import io.github.uveshm003.anchor.core.designsystem.component.AnchorTopAppBar
import io.github.uveshm003.anchor.core.designsystem.component.HoldToConfirmButton
import io.github.uveshm003.anchor.feature.home.R
import kotlin.time.Duration.Companion.seconds

@Composable
fun EmergencyUnlockRoute(onClose: () -> Unit, viewModel: EmergencyUnlockViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    EmergencyUnlockScreen(
        state = state,
        actions = EmergencyActions(
            onClose = onClose,
            onBegin = viewModel::begin,
            onHoldCompleted = viewModel::holdCompleted,
            onType = viewModel::type,
            onSubmit = viewModel::submit,
            onStartRecovery = viewModel::startRecovery,
        ),
    )
}

data class EmergencyActions(
    val onClose: () -> Unit = {},
    val onBegin: () -> Unit = {},
    val onHoldCompleted: () -> Unit = {},
    /** Returns whether the change is accepted (pastes are not). */
    val onType: (String) -> Boolean = { true },
    val onSubmit: (String) -> Unit = {},
    val onStartRecovery: () -> Unit = {},
)

@Composable
fun EmergencyUnlockScreen(state: EmergencyUiState, actions: EmergencyActions, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            AnchorTopAppBar(
                title = stringResource(R.string.feature_home_emergency_title),
                navigationIcon = {
                    AnchorIconButton(AnchorIcons.Close, stringResource(R.string.feature_home_close), actions.onClose)
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 560.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) { StepContent(state, actions) }
        }
    }
}

@Composable
private fun StepContent(state: EmergencyUiState, actions: EmergencyActions) {
    when (state.step) {
        EmergencyStep.Intro -> {
            Body(stringResource(R.string.feature_home_emergency_intro))
            Body(stringResource(R.string.feature_home_emergency_left, state.left, state.quota))
            AnchorButton(text = stringResource(R.string.feature_home_emergency_continue), onClick = actions.onBegin)
        }

        EmergencyStep.Hold -> {
            Body(stringResource(R.string.feature_home_emergency_hold_body))
            HoldToConfirmButton(
                text = stringResource(R.string.feature_home_emergency_hold),
                onConfirm = actions.onHoldCompleted,
                holdDuration = HOLD_SECONDS.seconds,
                accessibleHold = AccessibleHold.Countdown(),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        EmergencyStep.Type -> TypeSentence(state, actions)

        EmergencyStep.Done -> {
            Heading(stringResource(R.string.feature_home_emergency_done))
            AnchorButton(text = stringResource(R.string.feature_home_close), onClick = actions.onClose)
        }

        EmergencyStep.Exhausted -> {
            Heading(stringResource(R.string.feature_home_emergency_exhausted_title))
            Body(stringResource(R.string.feature_home_emergency_exhausted_body))
            if (state.recoveryPending) {
                Body(stringResource(R.string.feature_home_recovery_already_running))
            } else {
                Body(stringResource(R.string.feature_home_recovery_explain))
                AnchorButton(
                    text = stringResource(R.string.feature_home_recovery_start),
                    onClick = actions.onStartRecovery,
                )
            }
        }

        EmergencyStep.RecoveryStarted -> {
            Heading(stringResource(R.string.feature_home_recovery_started_title))
            Body(stringResource(R.string.feature_home_recovery_started_body))
            AnchorButton(text = stringResource(R.string.feature_home_close), onClick = actions.onClose)
        }

        EmergencyStep.NoSession -> {
            Body(stringResource(R.string.feature_home_emergency_no_session))
            AnchorButton(text = stringResource(R.string.feature_home_close), onClick = actions.onClose)
        }
    }
}

@Composable
private fun TypeSentence(state: EmergencyUiState, actions: EmergencyActions) {
    val sentences = stringArrayResource(R.array.feature_home_emergency_sentences)
    val sentence = sentences[state.sentenceIndex % sentences.size]
    // Local, synchronous text state: a field fed back through an async flow drops fast keystrokes.
    var typed by rememberSaveable { mutableStateOf(state.typed) }
    Body(stringResource(R.string.feature_home_emergency_type_body))
    Text(sentence, style = MaterialTheme.typography.titleMedium)
    // No text toolbar, so no paste; the ViewModel also ignores paste-sized changes.
    CompositionLocalProvider(LocalTextToolbar provides NoTextToolbar) {
        OutlinedTextField(
            value = typed,
            onValueChange = { if (actions.onType(it)) typed = it },
            label = { Text(stringResource(R.string.feature_home_emergency_type_label)) },
            isError = state.mismatch,
            supportingText = if (state.mismatch) {
                { Text(stringResource(R.string.feature_home_emergency_mismatch)) }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    AnchorButton(text = stringResource(R.string.feature_home_emergency_unlock_now), onClick = {
        actions.onSubmit(sentence)
    })
}

@Composable
private fun Body(text: String) = Text(text, style = MaterialTheme.typography.bodyLarge)

@Composable
private fun Heading(text: String) =
    Text(text, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })

private object NoTextToolbar : TextToolbar {
    override val status: TextToolbarStatus = TextToolbarStatus.Hidden

    override fun hide() = Unit

    override fun showMenu(
        rect: androidx.compose.ui.geometry.Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) = Unit
}

private const val HOLD_SECONDS = 10
