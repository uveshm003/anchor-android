package io.github.uveshm003.anchor

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.uveshm003.anchor.core.designsystem.theme.AnchorTheme
import io.github.uveshm003.anchor.feature.onboarding.OnboardingRoute
import io.github.uveshm003.anchor.service.blocker.ui.BlockActivity
import io.github.uveshm003.anchor.ui.AnchorApp
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    /** What the launching intent asked to open (e.g. emergency unlock from the block screen). */
    private val openRequest = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) openRequest.value = intent.getStringExtra(BlockActivity.EXTRA_OPEN)
        setContent {
            val open by openRequest.collectAsStateWithLifecycle()
            val showOnboarding by mainViewModel.showOnboarding.collectAsStateWithLifecycle()
            AnchorTheme {
                when (showOnboarding) {
                    null -> Unit

                    true -> OnboardingRoute()

                    false -> AnchorApp(
                        openEmergencyUnlock = open == BlockActivity.OPEN_EMERGENCY_UNLOCK,
                        onOpenHandle = { openRequest.value = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openRequest.value = intent.getStringExtra(BlockActivity.EXTRA_OPEN)
    }
}
