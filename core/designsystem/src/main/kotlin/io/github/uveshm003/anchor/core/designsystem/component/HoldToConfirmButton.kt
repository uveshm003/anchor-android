package io.github.uveshm003.anchor.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.uveshm003.anchor.core.designsystem.R
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * A button that confirms only after it is held for [holdDuration] (brief: 2 s hold-to-start, 10 s emergency hold).
 *
 * Progress is driven by frame timestamps, not an animation, so the hold takes the same time when system
 * animations are turned off. Haptics: a tick on press, light ticks at 25/50/75 % and a confirm effect at the end.
 * Releasing early cancels.
 *
 * TalkBack: the button exposes its progress as a state description. When [accessibilityActionLabel] is non-null,
 * a custom action with that label confirms immediately, because holding is hard with a screen reader. Pass `null`
 * where the hold itself is the friction and another step follows (e.g. the emergency unlock asks for typing).
 */
@Composable
fun HoldToConfirmButton(
    text: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    holdDuration: Duration = 2.seconds,
    enabled: Boolean = true,
    accessibilityActionLabel: String? = text,
) {
    val haptics = LocalHapticFeedback.current
    val currentOnConfirm by rememberUpdatedState(onConfirm)
    val scope = rememberCoroutineScope()
    var progress by remember { mutableFloatStateOf(0f) }

    val seconds = holdDuration.inWholeSeconds.toInt()
    val description = if (progress == 0f) {
        pluralStringResource(R.plurals.core_designsystem_hold_hint, seconds, seconds)
    } else {
        stringResource(R.string.core_designsystem_hold_progress, (progress * PERCENT).roundToInt())
    }

    HoldToConfirmButtonContent(
        text = text,
        progress = progress,
        enabled = enabled,
        modifier = modifier
            .semantics(mergeDescendants = true) {
                role = Role.Button
                stateDescription = description
                if (!enabled) disabled()
                if (enabled && accessibilityActionLabel != null) {
                    customActions = listOf(
                        CustomAccessibilityAction(accessibilityActionLabel) {
                            currentOnConfirm()
                            true
                        },
                    )
                }
            }
            .pointerInput(enabled, holdDuration) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                    val hold: Job = scope.launch {
                        holdFor(
                            duration = holdDuration,
                            onProgress = { progress = it },
                            onTick = { haptics.performHapticFeedback(HapticFeedbackType.SegmentTick) },
                        )
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        currentOnConfirm()
                    }
                    waitForUpOrCancellation()
                    hold.cancel()
                    progress = 0f
                }
            },
    )
}

/** Reports progress every frame until [duration] has elapsed, with a tick at each quarter. */
private suspend fun holdFor(duration: Duration, onProgress: (Float) -> Unit, onTick: () -> Unit) {
    val totalNanos = duration.inWholeNanoseconds.coerceAtLeast(1)
    val start = withFrameNanos { it }
    var nextTick = 1
    while (true) {
        val elapsed = withFrameNanos { it } - start
        val progress = (elapsed.toDouble() / totalNanos).toFloat().coerceIn(0f, 1f)
        onProgress(progress)
        while (nextTick < TICKS && progress >= nextTick.toFloat() / TICKS) {
            onTick()
            nextTick++
        }
        if (progress >= 1f) return
    }
}

/** Stateless rendering of the button at a given [progress], used by previews and screenshot tests. */
@Composable
internal fun HoldToConfirmButtonContent(
    text: String,
    progress: Float,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val container = if (enabled) colors.primaryContainer else colors.onSurface.copy(alpha = DISABLED_CONTAINER_ALPHA)
    val content = if (enabled) colors.onPrimaryContainer else colors.onSurface.copy(alpha = DISABLED_CONTENT_ALPHA)
    val fill = colors.primary.copy(alpha = FILL_ALPHA)
    Box(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(container)
            .drawBehind {
                val width = size.width * progress.coerceIn(0f, 1f)
                val left = if (layoutDirection == LayoutDirection.Rtl) size.width - width else 0f
                drawRect(fill, topLeft = Offset(left, 0f), size = Size(width, size.height))
            }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = content, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

private const val TICKS = 4
private const val PERCENT = 100
private const val FILL_ALPHA = 0.32f
private const val DISABLED_CONTAINER_ALPHA = 0.12f
private const val DISABLED_CONTENT_ALPHA = 0.38f
