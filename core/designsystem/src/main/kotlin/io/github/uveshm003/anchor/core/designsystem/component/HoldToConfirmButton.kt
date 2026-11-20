package io.github.uveshm003.anchor.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.uveshm003.anchor.core.designsystem.R
import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope

/**
 * A button that confirms only after it is held for [holdDuration] (brief: 2 s hold-to-start, 10 s emergency hold).
 *
 * - Progress uses frame timestamps, so the hold time is unchanged with system animations off. Releasing early
 *   cancels and animates the progress back.
 * - Haptics: on press, at each quarter and on confirm.
 * - Touch, keyboard/D-pad (Enter, Space, centre: press and hold the key) and accessibility services all work.
 *   TalkBack hears a hint, progress in quarters and "Confirmed"; see [AccessibleHold] for the custom action.
 */
@Composable
fun HoldToConfirmButton(
    text: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    holdDuration: Duration = 2.seconds,
    enabled: Boolean = true,
    accessibleHold: AccessibleHold = AccessibleHold.Immediate(),
    state: HoldToConfirmState = rememberHoldToConfirmState(),
) {
    require(holdDuration > Duration.ZERO) { "holdDuration must be positive, was $holdDuration" }
    val haptics by rememberUpdatedState(LocalHapticFeedback.current)
    val currentOnConfirm by rememberUpdatedState(onConfirm)
    val scope = rememberCoroutineScope()

    // A change of enabled/duration (or leaving composition) stops any running hold or countdown.
    DisposableEffect(state, enabled, holdDuration) { onDispose { state.reset() } }

    val controller = HoldController(
        state = state,
        scope = scope,
        begin = {
            state.start(
                scope = scope,
                duration = holdDuration,
                onTick = { haptics.performHapticFeedback(HapticFeedbackType.SegmentTick) },
                onComplete = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    currentOnConfirm()
                },
            )
        },
        press = { haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate) },
        confirmNow = { state.confirmNow { currentOnConfirm() } },
    )
    val labels = holdLabels(state, holdDuration, accessibleHold)

    HoldToConfirmButtonContent(
        text = text,
        progress = { state.progress },
        enabled = enabled,
        modifier = modifier
            .holdSemantics(enabled, accessibleHold, labels, controller)
            .holdKeyInput(enabled, controller)
            .focusable(enabled)
            .holdPointerInput(enabled, holdDuration, controller),
    )
}

/** The actions a [HoldToConfirmButton] wires to touch, keys and accessibility. */
private class HoldController(
    val state: HoldToConfirmState,
    val scope: CoroutineScope,
    val begin: () -> Unit,
    val press: () -> Unit,
    val confirmNow: () -> Unit,
) {
    fun release() = state.release(scope)
}

private class HoldLabels(val description: String, val action: String, val cancel: String)

@Composable
private fun holdLabels(state: HoldToConfirmState, holdDuration: Duration, accessibleHold: AccessibleHold): HoldLabels {
    val seconds = ceil(holdDuration.inWholeMilliseconds / MILLIS_PER_SECOND).toInt()
    val quarter by remember(state) { derivedStateOf { (state.progress * HOLD_TICKS).toInt() } }
    val description = when {
        state.confirmed -> stringResource(R.string.core_designsystem_hold_confirmed)

        state.isHolding || quarter > 0 ->
            stringResource(R.string.core_designsystem_hold_progress, quarter * PERCENT / HOLD_TICKS)

        else -> pluralStringResource(R.plurals.core_designsystem_hold_hint, seconds, seconds)
    }
    val action = when (accessibleHold) {
        is AccessibleHold.Immediate ->
            accessibleHold.label ?: stringResource(R.string.core_designsystem_hold_action_confirm)

        is AccessibleHold.Countdown ->
            accessibleHold.label
                ?: pluralStringResource(R.plurals.core_designsystem_hold_action_countdown, seconds, seconds)
    }
    return HoldLabels(description, action, stringResource(R.string.core_designsystem_hold_action_cancel))
}

private fun Modifier.holdSemantics(
    enabled: Boolean,
    accessibleHold: AccessibleHold,
    labels: HoldLabels,
    controller: HoldController,
): Modifier = semantics(mergeDescendants = true) {
    role = Role.Button
    stateDescription = labels.description
    liveRegion = LiveRegionMode.Polite
    when {
        !enabled -> disabled()

        accessibleHold is AccessibleHold.Countdown && controller.state.isHolding ->
            customActions = listOf(
                CustomAccessibilityAction(labels.cancel) {
                    controller.release()
                    true
                },
            )

        else -> customActions = listOf(
            CustomAccessibilityAction(labels.action) {
                if (accessibleHold is AccessibleHold.Countdown) controller.begin() else controller.confirmNow()
                true
            },
        )
    }
}

/** Enter, Space or D-pad centre: hold the key like a finger. */
private fun Modifier.holdKeyInput(enabled: Boolean, controller: HoldController): Modifier = onKeyEvent { event ->
    if (!enabled || event.key !in CONFIRM_KEYS) return@onKeyEvent false
    when (event.type) {
        KeyEventType.KeyDown -> if (!controller.state.isHolding) controller.begin()
        KeyEventType.KeyUp -> controller.release()
    }
    true
}

private fun Modifier.holdPointerInput(enabled: Boolean, holdDuration: Duration, controller: HoldController): Modifier =
    pointerInput(controller.state, enabled, holdDuration) {
        if (!enabled) return@pointerInput
        awaitEachGesture {
            awaitFirstDown().consume()
            controller.press()
            controller.begin()
            try {
                waitForUpOrCancellation()?.consume()
            } finally {
                controller.release()
            }
        }
    }

/**
 * Stateless rendering at a given [progress] (read in the draw phase only), used by previews and screenshot tests.
 * Progress is a track along the bottom edge in `onPrimaryContainer`, which keeps ≥ 3:1 against the container
 * and leaves the label's contrast untouched.
 */
@Composable
internal fun HoldToConfirmButtonContent(
    text: String,
    progress: () -> Float,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val container = if (enabled) colors.primaryContainer else colors.onSurface.copy(alpha = DISABLED_CONTAINER_ALPHA)
    val content = if (enabled) colors.onPrimaryContainer else colors.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA)
    val indicator = colors.onPrimaryContainer
    val track = colors.onPrimaryContainer.copy(alpha = TRACK_ALPHA)
    Box(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(container)
            .drawWithContent {
                drawContent()
                val value = progress().coerceIn(0f, 1f)
                if (!enabled || value == 0f) return@drawWithContent
                val inset = 24.dp.toPx()
                val barHeight = 4.dp.toPx()
                val top = size.height - 8.dp.toPx() - barHeight
                val width = size.width - 2 * inset
                val radius = CornerRadius(barHeight / 2)
                drawRoundRect(track, Offset(inset, top), Size(width, barHeight), radius)
                val filled = width * value
                val left = if (layoutDirection == LayoutDirection.Rtl) inset + width - filled else inset
                drawRoundRect(indicator, Offset(left, top), Size(filled, barHeight), radius)
            }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = content, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

private val CONFIRM_KEYS = setOf(Key.Enter, Key.NumPadEnter, Key.Spacebar, Key.DirectionCenter)
private const val MILLIS_PER_SECOND = 1000.0
private const val PERCENT = 100
private const val TRACK_ALPHA = 0.24f
private const val DISABLED_CONTAINER_ALPHA = 0.12f
private const val DISABLED_CONTENT_ALPHA = 0.38f
