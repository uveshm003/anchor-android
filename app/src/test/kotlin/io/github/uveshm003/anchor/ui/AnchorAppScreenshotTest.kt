package io.github.uveshm003.anchor.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.uveshm003.anchor.core.designsystem.theme.AnchorTheme
import io.github.uveshm003.anchor.core.testing.screenshot.captureThemedScreenshots
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnchorAppScreenshotTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun anchorApp() = composeRule.captureThemedScreenshots("anchor_app") { darkTheme ->
        AnchorTheme(darkTheme = darkTheme, dynamicColor = false) { AnchorApp() }
    }
}
