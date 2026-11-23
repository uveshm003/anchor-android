package io.github.uveshm003.anchor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.uveshm003.anchor.feature.modes.R as ModesR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** On-device smoke test: the real app (real Hilt graph) starts and renders. Run nightly in CI. */
@RunWith(AndroidJUnit4::class)
class AppLaunchSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appLaunches_andRendersShell() {
        val modes = composeRule.activity.getString(ModesR.string.feature_modes_title)
        composeRule.onNodeWithText(modes).assertIsDisplayed()
    }
}
