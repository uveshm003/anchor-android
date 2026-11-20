package io.github.uveshm003.anchor.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.requestFocus
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.uveshm003.anchor.core.designsystem.theme.AnchorTheme
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class HoldToConfirmButtonTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val haptics = RecordingHaptics()
    private val state = HoldToConfirmState()
    private var confirmed = 0

    private fun show(
        holdDuration: Duration = 2.seconds,
        enabled: () -> Boolean = { true },
        accessibleHold: AccessibleHold = AccessibleHold.Immediate(),
        wrap: @Composable (@Composable () -> Unit) -> Unit = { it() },
    ): SemanticsNodeInteraction {
        composeRule.setContent {
            AnchorTheme(dynamicColor = false) {
                CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                    wrap {
                        HoldToConfirmButton(
                            text = TEXT,
                            onConfirm = { confirmed++ },
                            holdDuration = holdDuration,
                            enabled = enabled(),
                            accessibleHold = accessibleHold,
                            state = state,
                            modifier = Modifier.testTag(TAG),
                        )
                    }
                }
            }
        }
        composeRule.mainClock.autoAdvance = false
        return composeRule.onNodeWithTag(TAG)
    }

    @Test
    fun holdingForTheDuration_confirmsOnceWithHaptics() {
        val button = show()

        button.performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(1_900)
        assertEquals("not yet at 1.9 s", 0, confirmed)
        composeRule.mainClock.advanceTimeBy(200)
        button.performTouchInput { up() }
        composeRule.mainClock.advanceTimeBy(500)

        assertEquals(1, confirmed)
        assertEquals(
            listOf(
                HapticFeedbackType.GestureThresholdActivate,
                HapticFeedbackType.SegmentTick,
                HapticFeedbackType.SegmentTick,
                HapticFeedbackType.SegmentTick,
                HapticFeedbackType.Confirm,
            ),
            haptics.performed,
        )
        assertEquals("Confirmed", button.stateDescription())
        assertEquals(LiveRegionMode.Polite, button.config(SemanticsProperties.LiveRegion))
    }

    @Test
    fun releasingAt75Percent_cancelsAndAnimatesBack() {
        val button = show()

        button.performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(1_500)
        assertTrue("about 75 %, was ${state.progress}", state.progress in 0.70f..0.80f)
        button.performTouchInput { up() }
        composeRule.mainClock.advanceTimeBy(48)
        assertTrue("animating back, was ${state.progress}", state.progress in 0.01f..0.74f)
        assertEquals(
            "no progress announcement after release",
            "Double-tap and hold for 2 seconds",
            button.stateDescription(),
        )
        composeRule.mainClock.advanceTimeBy(3_000)

        assertEquals(0f, state.progress)
        assertEquals(0, confirmed)
        assertTrue(HapticFeedbackType.Confirm !in haptics.performed)
        assertEquals("Double-tap and hold for 2 seconds", button.stateDescription())
    }

    @Test
    fun stateDescription_isAnnouncedInQuarters() {
        val button = show()
        button.performTouchInput { down(center) }

        composeRule.mainClock.advanceTimeBy(400)
        assertEquals("0% held", button.stateDescription())
        composeRule.mainClock.advanceTimeBy(700)
        assertEquals("50% held", button.stateDescription())

        button.performTouchInput { up() }
    }

    @Test
    fun hint_roundsPartialSecondsUp() {
        val button = show(holdDuration = 1_500.milliseconds)
        assertEquals("Double-tap and hold for 2 seconds", button.stateDescription())
    }

    @Test
    fun disablingMidHold_cancelsWithoutConfirming() {
        var enabled by mutableStateOf(true)
        val button = show(enabled = { enabled })

        button.performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(1_000)
        enabled = false
        composeRule.mainClock.advanceTimeBy(2_000)
        button.performTouchInput { up() }
        composeRule.mainClock.advanceTimeBy(500)

        assertEquals(0, confirmed)
        assertEquals(0f, state.progress)
    }

    @Test
    fun immediateAction_hasAClearLabelAndAnnouncesConfirmation() {
        val button = show()

        button.performCustomAccessibilityActionWithLabel("Confirm without holding")
        // The action writes state from the test thread: let snapshot changes apply, then run a few frames.
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(100)

        assertEquals(1, confirmed)
        assertEquals("Confirmed", button.stateDescription())
    }

    @Test
    fun immediateAction_usesACallerLabel() {
        val button = show(accessibleHold = AccessibleHold.Immediate(label = "Start Deep Work now"))
        button.performCustomAccessibilityActionWithLabel("Start Deep Work now")
        assertEquals(1, confirmed)
    }

    @Test
    fun countdownAction_keepsTheFullHoldAndCanBeCancelled() {
        val button = show(accessibleHold = AccessibleHold.Countdown())

        button.performCustomAccessibilityActionWithLabel("Start 2-second countdown")
        composeRule.mainClock.advanceTimeBy(1_000)
        assertEquals("countdown keeps the friction", 0, confirmed)
        button.performCustomAccessibilityActionWithLabel("Cancel countdown")
        composeRule.mainClock.advanceTimeBy(3_000)
        assertEquals(0, confirmed)

        button.performCustomAccessibilityActionWithLabel("Start 2-second countdown")
        composeRule.mainClock.advanceTimeBy(2_100)
        assertEquals(1, confirmed)
    }

    @Test
    fun keyboard_holdingEnterConfirmsOnceDespiteAutoRepeat() {
        val button = show()
        button.requestFocus()

        // advanceEventTime sends real key auto-repeats while Enter is held.
        button.performKeyInput {
            keyDown(Key.Enter)
            advanceEventTime(4_500)
            keyUp(Key.Enter)
        }
        composeRule.mainClock.advanceTimeBy(100)

        assertEquals(1, confirmed)
        assertEquals("Confirmed", button.stateDescription())
    }

    @Test
    fun enablingAgain_clearsAPastConfirmation() {
        var enabled by mutableStateOf(true)
        val button = show(enabled = { enabled })
        button.performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(2_100)
        button.performTouchInput { up() }
        assertEquals("Confirmed", button.stateDescription())

        // State written from the test thread: apply the snapshot, then run a few frames.
        enabled = false
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(100)
        assertEquals("kept while disabled", "Confirmed", button.stateDescription())
        enabled = true
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(100)

        assertEquals("Double-tap and hold for 2 seconds", button.stateDescription())
    }

    @Test
    fun pressDoesNotReachAClickableParent() {
        var parentClicks = 0
        val button = show(wrap = { content -> Box(Modifier.clickable { parentClicks++ }) { content() } })

        button.performTouchInput {
            down(center)
            up()
        }
        composeRule.mainClock.advanceTimeBy(500)

        assertEquals(0, parentClicks)
    }

    @Test
    fun disabled_ignoresHoldsAndHasNoAction() {
        val button = show(enabled = { false })

        button.performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(3_000)
        button.performTouchInput { up() }

        assertEquals(0, confirmed)
        assertTrue(haptics.performed.isEmpty())
        assertTrue(SemanticsProperties.Disabled in button.fetchSemanticsNode().config)
        assertNull(button.config(SemanticsActions.CustomActions))
    }

    private fun SemanticsNodeInteraction.stateDescription(): String? = config(SemanticsProperties.StateDescription)

    private fun <T> SemanticsNodeInteraction.config(key: androidx.compose.ui.semantics.SemanticsPropertyKey<T>): T? =
        fetchSemanticsNode().config.getOrNull(key)

    private class RecordingHaptics : HapticFeedback {
        val performed = mutableListOf<HapticFeedbackType>()

        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            performed += hapticFeedbackType
        }
    }

    private companion object {
        const val TAG = "hold"
        const val TEXT = "Hold to start Deep Work"
    }
}
