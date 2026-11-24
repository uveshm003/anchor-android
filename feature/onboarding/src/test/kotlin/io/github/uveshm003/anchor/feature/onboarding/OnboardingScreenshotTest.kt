package io.github.uveshm003.anchor.feature.onboarding

import io.github.uveshm003.anchor.core.designsystem.theme.AnchorTheme
import io.github.uveshm003.anchor.core.screenshottesting.ScreenshotRule
import io.github.uveshm003.anchor.core.screenshottesting.ScreenshotVariant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

@RunWith(ParameterizedRobolectricTestRunner::class)
class OnboardingScreenshotTest(variant: ScreenshotVariant) {
    @get:Rule
    val screenshot = ScreenshotRule(variant)

    private fun capture(name: String, step: OnboardingStep) = screenshot.capture(name) { dark ->
        AnchorTheme(darkTheme = dark, dynamicColor = false) { OnboardingScreen(step, OnboardingActions()) }
    }

    @Test
    fun welcome() = capture("onboarding_welcome", OnboardingStep.Welcome)

    @Test
    fun disclosure() = capture("onboarding_disclosure", OnboardingStep.Disclosure)

    @Test
    fun enable() = capture("onboarding_enable", OnboardingStep.Enable)

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants() = ScreenshotVariant.parameters()
    }
}
