package io.github.uveshm003.anchor

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.uveshm003.anchor.feature.onboarding.R as OnboardingR
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
        // A fresh install starts with onboarding.
        val welcome = composeRule.activity.getString(OnboardingR.string.feature_onboarding_welcome_title)
        composeRule.onNodeWithText(welcome).assertIsDisplayed()
    }
}
