package io.github.uveshm003.anchor.core.designsystem.component

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Observable state of a [HoldToConfirmButton]; hoist it to read [progress] (e.g. in tests). */
@Stable
class HoldToConfirmState internal constructor() {
    /** 0 → 1 while held; animates back to 0 after an early release. */
    var progress by mutableFloatStateOf(0f)
        private set

    /** True from the moment a hold completes until the next hold starts. */
    var confirmed by mutableStateOf(false)
        private set

    /** True while a hold or accessibility countdown is running. */
    var isHolding by mutableStateOf(false)
        private set

    private var job: Job? = null

    internal fun start(scope: CoroutineScope, duration: Duration, onTick: () -> Unit, onComplete: () -> Unit) {
        job?.cancel()
        confirmed = false
        isHolding = true
        job = scope.launch {
            try {
                holdFor(duration, onProgress = { progress = it }, onTick = onTick)
                confirmed = true
                onComplete()
            } finally {
                isHolding = false
            }
        }
    }

    /** Ends a hold: cancels it if incomplete and animates the progress back (snaps when animations are off). */
    internal fun release(scope: CoroutineScope) {
        job?.cancel()
        isHolding = false
        if (confirmed || progress == 0f) {
            progress = 0f
            return
        }
        val from = progress
        job = scope.launch {
            animate(from, 0f, animationSpec = tween(RELEASE_MILLIS)) { value, _ -> progress = value }
        }
    }

    /** Accessibility "confirm without holding": marks the hold confirmed and runs [onConfirm]. */
    internal fun confirmNow(onConfirm: () -> Unit) {
        job?.cancel()
        isHolding = false
        progress = 0f
        confirmed = true
        onConfirm()
    }

    internal fun reset() {
        job?.cancel()
        job = null
        isHolding = false
        progress = 0f
    }

    private companion object {
        const val RELEASE_MILLIS = 200
    }
}

@Composable
fun rememberHoldToConfirmState(): HoldToConfirmState = remember { HoldToConfirmState() }

/**
 * Reports progress every frame until [duration] has elapsed, with a tick at each quarter. Uses frame
 * timestamps, not an animation, so the system animator scale can't shorten it.
 */
private suspend fun holdFor(duration: Duration, onProgress: (Float) -> Unit, onTick: () -> Unit) {
    val totalNanos = duration.inWholeNanoseconds
    val start = withFrameNanos { it }
    var nextTick = 1
    while (true) {
        val elapsed = withFrameNanos { it } - start
        val progress = (elapsed.toDouble() / totalNanos).toFloat().coerceIn(0f, 1f)
        onProgress(progress)
        while (nextTick < HOLD_TICKS && progress >= nextTick.toFloat() / HOLD_TICKS) {
            onTick()
            nextTick++
        }
        if (progress >= 1f) return
    }
}

internal const val HOLD_TICKS = 4
