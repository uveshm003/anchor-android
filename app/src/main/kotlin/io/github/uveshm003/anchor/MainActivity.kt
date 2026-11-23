package io.github.uveshm003.anchor

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.uveshm003.anchor.core.designsystem.theme.AnchorTheme
import io.github.uveshm003.anchor.core.nfc.AnchorTags
import io.github.uveshm003.anchor.core.nfc.NfcReaderMode
import io.github.uveshm003.anchor.service.blocker.ui.BlockActivity
import io.github.uveshm003.anchor.ui.AnchorApp
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var anchorTags: AnchorTags

    /** What the launching intent asked to open (e.g. emergency unlock from the block screen). */
    private val openRequest = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) openRequest.value = intent.getStringExtra(BlockActivity.EXTRA_OPEN)
        setContent {
            val open by openRequest.collectAsStateWithLifecycle()
            AnchorTheme {
                AnchorApp(
                    openEmergencyUnlock = open == BlockActivity.OPEN_EMERGENCY_UNLOCK,
                    onOpenHandle = { openRequest.value = null },
                )
            }
        }
    }

    /** While Anchor is open, NFC taps come through reader mode (to pairing or the engine). */
    override fun onResume() {
        super.onResume()
        NfcReaderMode.enable(this, anchorTags)
    }

    override fun onPause() {
        NfcReaderMode.disable(this)
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openRequest.value = intent.getStringExtra(BlockActivity.EXTRA_OPEN)
    }
}
