package io.github.uveshm003.anchor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.uveshm003.anchor.core.data.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Whether to show first-run onboarding (null while settings load). */
@HiltViewModel
class MainViewModel @Inject constructor(settings: SettingsRepository) : ViewModel() {
    val showOnboarding: StateFlow<Boolean?> = settings.settings
        .map { !it.onboardingCompleted }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
