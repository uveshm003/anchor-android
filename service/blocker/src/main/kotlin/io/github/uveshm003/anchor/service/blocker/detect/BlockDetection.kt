package io.github.uveshm003.anchor.service.blocker.detect

import io.github.uveshm003.anchor.core.model.AppPolicy
import io.github.uveshm003.anchor.core.model.SessionId

enum class WindowKind { Application, InputMethod, System, AccessibilityOverlay, SplitScreenDivider, Other }

/** One window from `AccessibilityService.getWindows()`, reduced to what blocking needs. */
data class VisibleWindow(val packageName: String?, val kind: WindowKind, val pictureInPicture: Boolean = false)

/** All windows on screen: split-screen, PiP, bubbles and freeform/desktop windows are all application windows. */
data class WindowSnapshot(val windows: List<VisibleWindow>) {
    /** Packages the user can see as apps (keyboards, system bars and overlays are not apps). */
    fun visiblePackages(): Set<String> =
        windows.filter { it.kind == WindowKind.Application }.mapNotNull { it.packageName }.toSet()
}

/** Which visible packages to block. Pure; the service only forwards to it. */
object BlockDecider {
    /** Never blocked, whatever the policy says (defence in depth on top of the engine's essentials). */
    private val ALWAYS_ALLOWED = setOf("android", "com.android.systemui")

    fun blocked(visible: Set<String>, policy: AppPolicy, ownPackage: String): Set<String> =
        visible.filterTo(mutableSetOf()) { it != ownPackage && it !in ALWAYS_ALLOWED && policy.blocks(it) }
}

/** Counts a block attempt once per foreground change, not once per accessibility event. */
class BlockAttemptDebouncer {
    private var session: SessionId? = null
    private var lastBlocked: Set<String> = emptySet()

    /** The blocked packages that weren't blocked-and-visible at the previous call. */
    fun newlyBlocked(sessionId: SessionId, blocked: Set<String>): Set<String> {
        if (sessionId != session) {
            session = sessionId
            lastBlocked = emptySet()
        }
        val fresh = blocked - lastBlocked
        lastBlocked = blocked
        return fresh
    }
}
