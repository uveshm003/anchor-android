package io.github.uveshm003.anchor.core.screenshottesting

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.github.takahirom.roborazzi.captureRoboImage
import java.time.Duration
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * Screenshot one [variant] in a **fresh activity**: qualifiers are applied before the compose rule launches
 * the activity, so no composition, `remember`/`rememberSaveable` state or ViewModel is shared between variants.
 *
 * ```
 * @RunWith(ParameterizedRobolectricTestRunner::class)
 * class MyScreenScreenshotTest(variant: ScreenshotVariant) {
 *     @get:Rule val screenshot = ScreenshotRule(variant)
 *
 *     @Test fun myScreen() = screenshot.capture("my_screen") { dark ->
 *         AnchorTheme(darkTheme = dark, dynamicColor = false) { MyScreen() }
 *     }
 *
 *     companion object {
 *         @JvmStatic @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
 *         fun variants() = ScreenshotVariant.parameters()
 *     }
 * }
 * ```
 *
 * Limits: the content view is drawn with a software canvas, so other windows (dialogs, popups, bottom sheets)
 * and elevation shadows (#84) are not captured.
 */
class ScreenshotRule(val variant: ScreenshotVariant) : TestRule {
    val composeRule: AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity> =
        createAndroidComposeRule<ComponentActivity>()

    private val qualifiers = object : ExternalResource() {
        override fun before() = RuntimeEnvironment.setQualifiers(variant.qualifiers)
    }

    override fun apply(base: Statement, description: Description): Statement =
        RuleChain.outerRule(qualifiers).around(composeRule).apply(base, description)

    /** Renders [content] and records/verifies `src/test/screenshots/<name>_<variant>.png`. */
    fun capture(name: String, content: @Composable (darkTheme: Boolean) -> Unit) {
        render(content)
        drawContent().captureRoboImage("src/test/screenshots/${name}_$variant.png")
    }

    /** Sets [content] and lets composition, layout and a frame finish. */
    fun render(content: @Composable (darkTheme: Boolean) -> Unit) {
        composeRule.activity.setContent { content(variant.darkTheme) }
        composeRule.waitForIdle()
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(FRAME_SETTLE_MILLIS))
        composeRule.waitForIdle()
    }

    /**
     * Draws the content view into a bitmap synchronously. Capturing through PixelCopy (result delivered via a
     * posted callback) produced fully transparent images under ciCheck load (#86).
     */
    private fun drawContent(): Bitmap {
        lateinit var bitmap: Bitmap
        composeRule.runOnUiThread {
            val content = composeRule.activity.findViewById<ViewGroup>(android.R.id.content)
            val root = checkNotNull(content.getChildAt(0)) { "Activity has no content view" }
            check(root.width > 0 && root.height > 0) { "Content view is not laid out (${root.width}x${root.height})" }
            bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
            root.draw(Canvas(bitmap))
        }
        return bitmap
    }

    private companion object {
        const val FRAME_SETTLE_MILLIS = 100L
    }
}
