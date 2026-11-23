package io.github.uveshm003.anchor.service.blocker

import com.google.common.truth.Truth.assertThat
import io.github.uveshm003.anchor.core.engine.Command
import io.github.uveshm003.anchor.core.engine.ExecutionResult
import io.github.uveshm003.anchor.core.model.AppPolicy
import io.github.uveshm003.anchor.core.model.ExitRule
import io.github.uveshm003.anchor.core.model.SessionId
import io.github.uveshm003.anchor.service.blocker.detect.BlockAttemptDebouncer
import io.github.uveshm003.anchor.service.blocker.detect.BlockDecider
import io.github.uveshm003.anchor.service.blocker.detect.VisibleWindow
import io.github.uveshm003.anchor.service.blocker.detect.WindowKind
import io.github.uveshm003.anchor.service.blocker.detect.WindowSnapshot
import io.github.uveshm003.anchor.service.blocker.policy.ActivePolicyStore
import io.github.uveshm003.anchor.service.blocker.policy.PolicyExecutor
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Test

class BlockDetectionTest {
    private val own = "io.github.uveshm003.anchor"
    private val block = AppPolicy.BlockList(setOf("com.social", "com.video"))
    private val allow = AppPolicy.AllowList(setOf("com.maps", "com.launcher"))

    private fun app(pkg: String, pip: Boolean = false) = VisibleWindow(pkg, WindowKind.Application, pip)

    /** Window arrangements as `getWindows()` reports them. */
    private val fixtures = mapOf(
        "fullscreen" to
            WindowSnapshot(listOf(app("com.social"), VisibleWindow("com.android.systemui", WindowKind.System))),
        "split screen" to WindowSnapshot(
            listOf(app("com.maps"), VisibleWindow(null, WindowKind.SplitScreenDivider), app("com.video")),
        ),
        "picture in picture" to WindowSnapshot(listOf(app("com.launcher"), app("com.video", pip = true))),
        "bubble over an app" to WindowSnapshot(listOf(app("com.maps"), app("com.social"))),
        "desktop windows" to
            WindowSnapshot(listOf(app("com.maps"), app("com.social"), app("com.video"), app("com.notes"))),
        "keyboard and overlay" to WindowSnapshot(
            listOf(
                app("com.maps"),
                VisibleWindow("com.keyboard", WindowKind.InputMethod),
                VisibleWindow("com.a11y", WindowKind.AccessibilityOverlay),
            ),
        ),
        "anchor itself" to WindowSnapshot(listOf(app(own))),
    )

    @Test
    fun visible_packages_cover_every_application_window_only() {
        assertThat(fixtures.getValue("split screen").visiblePackages()).containsExactly("com.maps", "com.video")
        assertThat(
            fixtures.getValue("picture in picture").visiblePackages(),
        ).containsExactly("com.launcher", "com.video")
        assertThat(fixtures.getValue("keyboard and overlay").visiblePackages()).containsExactly("com.maps")
        assertThat(fixtures.getValue("fullscreen").visiblePackages()).containsExactly("com.social")
    }

    @Test
    fun block_list_decisions_for_each_arrangement() {
        val expected = mapOf(
            "fullscreen" to setOf("com.social"),
            "split screen" to setOf("com.video"),
            "picture in picture" to setOf("com.video"),
            "bubble over an app" to setOf("com.social"),
            "desktop windows" to setOf("com.social", "com.video"),
            "keyboard and overlay" to emptySet(),
            "anchor itself" to emptySet(),
        )
        fixtures.forEach { (name, snapshot) ->
            assertThat(BlockDecider.blocked(snapshot.visiblePackages(), block, own)).isEqualTo(expected.getValue(name))
        }
    }

    @Test
    fun allow_list_never_blocks_anchor_or_system_ui() {
        val visible = setOf(own, "com.android.systemui", "android", "com.maps", "com.social")

        assertThat(BlockDecider.blocked(visible, allow, own)).containsExactly("com.social")
    }

    @Test
    fun attempts_count_once_per_foreground_change_and_reset_per_session() {
        val debouncer = BlockAttemptDebouncer()
        val s1 = SessionId(1)

        assertThat(debouncer.newlyBlocked(s1, setOf("com.social"))).containsExactly("com.social")
        assertThat(debouncer.newlyBlocked(s1, setOf("com.social"))).isEmpty()
        assertThat(debouncer.newlyBlocked(s1, setOf("com.social", "com.video"))).containsExactly("com.video")
        assertThat(debouncer.newlyBlocked(s1, emptySet())).isEmpty()
        assertThat(debouncer.newlyBlocked(s1, setOf("com.social"))).containsExactly("com.social")
        assertThat(debouncer.newlyBlocked(SessionId(2), setOf("com.social"))).containsExactly("com.social")
    }

    @Test
    fun policy_executor_applies_and_clears() = runTest {
        val store = ActivePolicyStore()
        val executor = PolicyExecutor(store)
        val start = Instant.parse("2026-11-23T10:00:00Z")

        executor.execute(
            Command.ApplyAppPolicy("a", SessionId(3), "Work", block, start, ExitRule.RequiresAnchor(), null, "Desk"),
        )
        assertThat(store.active.value?.modeName).isEqualTo("Work")
        assertThat(store.active.value?.requiredAnchorName).isEqualTo("Desk")

        assertThat(executor.execute(Command.ClearAppPolicy("c"))).isEqualTo(ExecutionResult.Done)
        assertThat(store.active.value).isNull()
        assertThat(
            executor.execute(Command.CancelTimer("x", SessionId(3))),
        ).isInstanceOf(ExecutionResult.Failed::class.java)
    }
}
