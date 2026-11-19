package io.github.uveshm003.anchor.core.designsystem.component

import android.provider.Settings
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.uveshm003.anchor.core.designsystem.theme.AnchorTheme
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HoldToConfirmButtonTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val haptics = RecordingHaptics()
    private var confirmed = 0

    private fun show(
        holdDuration: Duration = 2.seconds,
        enabled: Boolean = true,
        accessibilityActionLabel: String? = LABEL,
    ): SemanticsNodeInteraction {
        composeRule.setContent {
            AnchorTheme(dynamicColor = false) {
                CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                    HoldToConfirmButton(
                        text = LABEL,
                        onConfirm = { confirmed++ },
                        holdDuration = holdDuration,
                        enabled = enabled,
                        accessibilityActionLabel = accessibilityActionLabel,
                        modifier = Modifier.testTag(TAG),
                    )
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
    }

    @Test
    fun releasingAt75Percent_cancels() {
        val button = show()

        button.performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(1_500)
        button.performTouchInput { up() }
        composeRule.mainClock.advanceTimeBy(3_000)

        assertEquals(0, confirmed)
        assertTrue(HapticFeedbackType.Confirm !in haptics.performed)
        assertEquals("Hold for 2 seconds", button.stateDescription())
    }

    @Test
    fun stateDescription_reportsProgressWhileHeld() {
        val button = show()
        assertEquals("Hold for 2 seconds", button.stateDescription())

        button.performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(1_000)

        val percent = button.stateDescription()!!.removeSuffix("% held").toInt()
        assertTrue("about half: $percent", percent in 45..55)
        button.performTouchInput { up() }
    }

    @Test
    fun holdTime_isUnchangedWhenAnimationsAreOff() {
        val resolver = ApplicationProvider.getApplicationContext<android.content.Context>().contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        val button = show()

        button.performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(1_000)
        assertEquals("animations off must not shorten the hold", 0, confirmed)
        composeRule.mainClock.advanceTimeBy(1_100)
        button.performTouchInput { up() }

        assertEquals(1, confirmed)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun accessibilityAction_confirmsImmediately() {
        val button = show()
        button.performCustomAccessibilityActionWithLabel(LABEL)
        assertEquals(1, confirmed)
    }

    @Test
    fun noAccessibilityAction_whenLabelIsNull() {
        val button = show(accessibilityActionLabel = null)
        assertNull(button.fetchSemanticsNode().config.getOrNull(SemanticsActions.CustomActions))
    }

    @Test
    fun disabled_ignoresHoldsAndHasNoAction() {
        val button = show(enabled = false)

        button.performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(3_000)
        button.performTouchInput { up() }

        assertEquals(0, confirmed)
        assertTrue(haptics.performed.isEmpty())
        assertTrue(SemanticsProperties.Disabled in button.fetchSemanticsNode().config)
        assertNull(button.fetchSemanticsNode().config.getOrNull(SemanticsActions.CustomActions))
    }

    private fun SemanticsNodeInteraction.stateDescription(): String? =
        fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription)

    private class RecordingHaptics : HapticFeedback {
        val performed = mutableListOf<HapticFeedbackType>()

        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            performed += hapticFeedbackType
        }
    }

    private companion object {
        const val TAG = "hold"
        const val LABEL = "Start Deep Work"
    }
}
