package io.github.uveshm003.anchor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import io.github.uveshm003.anchor.ui.AnchorApp
import io.github.uveshm003.anchor.ui.theme.AnchorTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnchorTheme {
                AnchorApp()
            }
        }
    }
}
