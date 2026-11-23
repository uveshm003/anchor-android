package io.github.uveshm003.anchor.service.blocker.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.uveshm003.anchor.core.designsystem.theme.AnchorTheme
import io.github.uveshm003.anchor.service.blocker.policy.ActivePolicyStore
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.delay

/** Full-screen block screen (ADR 0006). Back and "Go home" go to the launcher; it closes when the session ends. */
@AndroidEntryPoint
class BlockActivity : ComponentActivity() {
    @Inject lateinit var store: ActivePolicyStore

    @Inject lateinit var clock: Clock

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = goHome()
            },
        )
        setContent {
            val policy by store.active.collectAsStateWithLifecycle()
            val now by produceState(clock.instant()) {
                while (true) {
                    delay(TICK_MS)
                    value = clock.instant()
                }
            }
            LaunchedEffect(policy) { if (policy == null) finish() }
            AnchorTheme {
                policy?.let {
                    BlockScreen(it, now, clock.zone, onGoHome = ::goHome, onEmergencyUnlock = ::openEmergencyUnlock)
                }
            }
        }
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        finish()
    }

    /** Opens Anchor on its emergency-unlock entry (the app handles [EXTRA_OPEN]). */
    private fun openEmergencyUnlock() {
        packageManager.getLaunchIntentForPackage(packageName)
            ?.putExtra(EXTRA_OPEN, OPEN_EMERGENCY_UNLOCK)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            ?.let(::startActivity)
        finish()
    }

    companion object {
        const val EXTRA_OPEN = "io.github.uveshm003.anchor.extra.OPEN"
        const val OPEN_EMERGENCY_UNLOCK = "emergency_unlock"
        private const val TICK_MS = 30_000L
    }
}
