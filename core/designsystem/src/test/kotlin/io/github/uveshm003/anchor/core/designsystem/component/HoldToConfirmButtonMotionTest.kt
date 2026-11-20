package io.github.uveshm003.anchor.core.designsystem.component

import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.uveshm003.anchor.core.designsystem.theme.AnchorTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** "Animations off" for real: the composition's MotionDurationScale is 0, as with the system setting at 0×. */
@RunWith(AndroidJUnit4::class)
class HoldToConfirmButtonMotionTest {
    @get:Rule
    val composeRule = createComposeRule(
        effectContext = object : MotionDurationScale {
            override val scaleFactor: Float = 0f
        },
    )

    @Test
    fun holdTime_isUnchanged_andReleaseSnapsBack() {
        val state = HoldToConfirmState()
        var confirmed = 0
        composeRule.setContent {
            AnchorTheme(dynamicColor = false) {
                HoldToConfirmButton("Hold", onConfirm = {
                    confirmed++
                }, state = state, modifier = Modifier.testTag("hold"))
            }
        }
        composeRule.mainClock.autoAdvance = false
        val button = composeRule.onNodeWithTag("hold")

        button.performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(1_000)
        assertEquals("animations off must not shorten the hold", 0, confirmed)
        button.performTouchInput { up() }
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals("release snaps back with animations off", 0f, state.progress)

        button.performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(2_100)
        button.performTouchInput { up() }
        assertEquals(1, confirmed)
    }
}
