package io.github.uveshm003.anchor.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.uveshm003.anchor.core.testing.MainDispatcherRule
import io.github.uveshm003.anchor.core.testing.data.FakeBlockerStatus
import io.github.uveshm003.anchor.core.testing.data.FakeSettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OnboardingTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val settings = FakeSettingsRepository()
    private val blocker = FakeBlockerStatus(enabled = false)

    @Test
    fun welcome_disclosure_consent_then_finishes_when_the_blocker_is_turned_on() = runTest {
        val handle = SavedStateHandle()
        val vm = OnboardingViewModel(handle, settings, blocker)
        assertThat(vm.step.value).isEqualTo(OnboardingStep.Welcome)

        vm.next()
        assertThat(vm.step.value).isEqualTo(OnboardingStep.Disclosure)
        vm.effects.test {
            vm.agree()
            assertThat(awaitItem()).isEqualTo(OnboardingEffect.OpenAccessibilitySettings)
            vm.openSettings()
            assertThat(awaitItem()).isEqualTo(OnboardingEffect.OpenAccessibilitySettings)
        }
        assertThat(vm.step.value).isEqualTo(OnboardingStep.Enable)
        assertThat(settings.settings.value.onboardingCompleted).isFalse()

        // The step survives process death.
        assertThat(OnboardingViewModel(handle, settings, blocker).step.value).isEqualTo(OnboardingStep.Enable)

        blocker.enabled.value = true
        assertThat(settings.settings.value.onboardingCompleted).isTrue()
    }

    @Test
    fun not_now_finishes_without_the_blocker() = runTest {
        val vm = OnboardingViewModel(SavedStateHandle(), settings, blocker)

        vm.notNow()

        assertThat(settings.settings.value.onboardingCompleted).isTrue()
    }
}
