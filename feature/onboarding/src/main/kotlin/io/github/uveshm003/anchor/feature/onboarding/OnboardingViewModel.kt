package io.github.uveshm003.anchor.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.uveshm003.anchor.core.data.platform.BlockerStatus
import io.github.uveshm003.anchor.core.data.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

enum class OnboardingStep { Welcome, Disclosure, Enable }

sealed interface OnboardingEffect {
    /** The user consented: open system Accessibility settings. */
    data object OpenAccessibilitySettings : OnboardingEffect
}

/**
 * First run: welcome → prominent disclosure → explicit consent → Accessibility settings. Finishes by itself once the
 * blocker service is on; "Not now" also finishes (the app stays usable and Home shows a banner). No other
 * permission is asked for here.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val settings: SettingsRepository,
    blocker: BlockerStatus,
) : ViewModel() {
    val step: StateFlow<OnboardingStep> = savedState.getStateFlow(STEP_KEY, OnboardingStep.Welcome)

    private val _effects = Channel<OnboardingEffect>(Channel.BUFFERED)
    val effects: Flow<OnboardingEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            blocker.enabled.first { it }
            finish()
        }
    }

    fun next() {
        savedState[STEP_KEY] = OnboardingStep.Disclosure
    }

    /** Explicit consent to the disclosure. */
    fun agree() {
        savedState[STEP_KEY] = OnboardingStep.Enable
        viewModelScope.launch { _effects.send(OnboardingEffect.OpenAccessibilitySettings) }
    }

    fun openSettings() {
        viewModelScope.launch { _effects.send(OnboardingEffect.OpenAccessibilitySettings) }
    }

    fun notNow() = finish()

    private fun finish() {
        viewModelScope.launch { settings.setOnboardingCompleted(true) }
    }

    private companion object {
        const val STEP_KEY = "step"
    }
}
