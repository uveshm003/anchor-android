package io.github.uveshm003.anchor.ui

import android.os.Looper
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.uveshm003.anchor.core.designsystem.theme.AnchorTheme
import java.time.Duration
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class AnchorAppScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    @Config(qualifiers = "w411dp-h891dp-notnight-xxhdpi")
    fun anchorApp_phone_light() = capture("anchor_app_phone_light", darkTheme = false)

    @Test
    @Config(qualifiers = "w411dp-h891dp-night-xxhdpi")
    fun anchorApp_phone_dark() = capture("anchor_app_phone_dark", darkTheme = true)

    @Test
    @Config(qualifiers = "w1280dp-h800dp-notnight-xhdpi")
    fun anchorApp_tablet_light() = capture("anchor_app_tablet_light", darkTheme = false)

    @Test
    @Config(qualifiers = "w1280dp-h800dp-night-xhdpi")
    fun anchorApp_tablet_dark() = capture("anchor_app_tablet_dark", darkTheme = true)

    private fun capture(name: String, darkTheme: Boolean) {
        composeRule.setContent {
            AnchorTheme(darkTheme = darkTheme, dynamicColor = false) { AnchorApp() }
        }
        // Compose being idle doesn't guarantee a frame reached the window surface Roborazzi reads. Advance the
        // Robolectric main-looper clock a few frames so a traversal + draw has run (blank captures under load).
        composeRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(FRAME_SETTLE_MILLIS))
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    private companion object {
        const val FRAME_SETTLE_MILLIS = 100L
    }
}
