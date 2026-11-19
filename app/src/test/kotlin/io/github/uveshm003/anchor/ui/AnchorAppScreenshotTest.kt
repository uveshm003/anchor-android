package io.github.uveshm003.anchor.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
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
    val composeRule = createAndroidComposeRule<ComponentActivity>()

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
        composeRule.captureContent("src/test/screenshots/$name.png")
    }

    private companion object {
        const val FRAME_SETTLE_MILLIS = 100L
    }
}

/**
 * Draws the content view synchronously. Capturing via PixelCopy (async callback) produced transparent images
 * under ciCheck load (#86). Temporary per-file copy until the shared screenshot helper lands (#88).
 */
private fun AndroidComposeTestRule<*, out ComponentActivity>.captureContent(path: String) {
    lateinit var bitmap: Bitmap
    runOnUiThread {
        val root = checkNotNull(activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)) {
            "Activity has no content view"
        }
        check(root.width > 0 && root.height > 0) { "Content view is not laid out (${root.width}x${root.height})" }
        bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
    }
    bitmap.captureRoboImage(path)
}
