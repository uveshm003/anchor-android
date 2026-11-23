package io.github.uveshm003.anchor.service.blocker

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import dagger.hilt.android.AndroidEntryPoint
import io.github.uveshm003.anchor.core.common.dispatchers.ApplicationScope
import io.github.uveshm003.anchor.core.engine.BlockAttemptRecorder
import io.github.uveshm003.anchor.service.blocker.detect.BlockAttemptDebouncer
import io.github.uveshm003.anchor.service.blocker.detect.BlockDecider
import io.github.uveshm003.anchor.service.blocker.detect.VisibleWindow
import io.github.uveshm003.anchor.service.blocker.detect.WindowKind
import io.github.uveshm003.anchor.service.blocker.detect.WindowSnapshot
import io.github.uveshm003.anchor.service.blocker.policy.ActivePolicyStore
import io.github.uveshm003.anchor.service.blocker.ui.BlockActivity
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Detects blocked apps in **all** windows (split-screen, PiP, bubbles, desktop windows) and shows the block
 * screen (ADR 0006). Returns immediately when no session is active. No business logic: the engine decides the
 * policy, [BlockDecider] decides what to block.
 */
@AndroidEntryPoint
class BlockerService : AccessibilityService() {
    @Inject lateinit var store: ActivePolicyStore

    @Inject lateinit var recorder: BlockAttemptRecorder

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    private val debouncer = BlockAttemptDebouncer()

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val active = store.active.value ?: return
        val visible = snapshot().visiblePackages().ifEmpty { setOfNotNull(event.packageName?.toString()) }
        val blocked = BlockDecider.blocked(visible, active.policy, packageName)
        debouncer.newlyBlocked(active.sessionId, blocked).forEach { pkg ->
            scope.launch { recorder.record(active.sessionId, pkg) }
        }
        if (blocked.isNotEmpty()) {
            startActivity(
                Intent(this, BlockActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            )
        }
    }

    override fun onInterrupt() = Unit

    private fun snapshot() = WindowSnapshot(
        windows.map { window ->
            VisibleWindow(
                packageName = window.root?.packageName?.toString(),
                kind = window.type.toKind(),
                pictureInPicture = window.isInPictureInPictureMode,
            )
        },
    )

    private fun Int.toKind() = when (this) {
        AccessibilityWindowInfo.TYPE_APPLICATION -> WindowKind.Application
        AccessibilityWindowInfo.TYPE_INPUT_METHOD -> WindowKind.InputMethod
        AccessibilityWindowInfo.TYPE_SYSTEM -> WindowKind.System
        AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY -> WindowKind.AccessibilityOverlay
        AccessibilityWindowInfo.TYPE_SPLIT_SCREEN_DIVIDER -> WindowKind.SplitScreenDivider
        else -> WindowKind.Other
    }
}
