package io.github.uveshm003.anchor.core.testing.screenshot

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import com.github.takahirom.roborazzi.captureRoboImage
import java.time.Duration
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * Captures [content] on each of [devices] in light and dark, as
 * `src/test/screenshots/<name>_<device>_<light|dark>.png`.
 *
 * Use with `createAndroidComposeRule<ComponentActivity>()`. [content] receives `darkTheme` and should wrap
 * itself in the app theme, e.g. `{ dark -> AnchorTheme(darkTheme = dark, dynamicColor = false) { MyScreen() } }`.
 */
fun AndroidComposeTestRule<*, out ComponentActivity>.captureThemedScreenshots(
    name: String,
    devices: List<ScreenshotDevice> = ScreenshotDevice.entries,
    content: @Composable (darkTheme: Boolean) -> Unit,
) {
    for (device in devices) {
        for (darkTheme in listOf(false, true)) {
            val night = if (darkTheme) "night" else "notnight"
            RuntimeEnvironment.setQualifiers("${device.size}-$night-${device.density}")
            activity.setContent { content(darkTheme) }
            settleFrame()
            val variant = if (darkTheme) "dark" else "light"
            drawContent().captureRoboImage("src/test/screenshots/${name}_${device.name.lowercase()}_$variant.png")
        }
    }
}

/** Let composition, layout and a frame finish before drawing. */
private fun AndroidComposeTestRule<*, *>.settleFrame() {
    waitForIdle()
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(FRAME_SETTLE_MILLIS))
    waitForIdle()
}

/**
 * Draws the content view into a bitmap synchronously on the test thread. Capturing through PixelCopy
 * (which delivers its result via a posted callback) produced fully transparent images under ciCheck load
 * for text-heavy screens (#2), so the capture must not depend on async delivery.
 */
private fun AndroidComposeTestRule<*, out ComponentActivity>.drawContent(): Bitmap {
    lateinit var bitmap: Bitmap
    runOnUiThread {
        val root: View = activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        check(root.width > 0 && root.height > 0) { "Content view is not laid out (${root.width}x${root.height})" }
        bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
    }
    return bitmap
}

private const val FRAME_SETTLE_MILLIS = 100L
