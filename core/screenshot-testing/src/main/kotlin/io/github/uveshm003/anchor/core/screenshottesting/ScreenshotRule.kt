package io.github.uveshm003.anchor.core.screenshottesting

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.ViewGroup
import androidx.activity.ComponentActivity
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
 * Golden files are `src/test/screenshots/<name>[_<locale>]_<variant>.png`; the locale part is added for any
 * non-English class-level locale, so Hindi and English captures of the same screen don't collide. For any other
 * per-class configuration (font scale, `ldrtl`, …) give `name` a distinct suffix, e.g. `home_fontscale2`.
 *
 * The host is `ComponentActivity` by default; pass another [activityClass] (e.g. a Hilt test activity, with
 * `HiltAndroidRule` ordered outside this rule) to capture composables that need it. Prefer capturing the
 * stateless screen composable with fake state.
 *
 * Limits: the content view is drawn with a software canvas, so other windows (dialogs, popups, bottom sheets)
 * and elevation shadows (#84) are not captured.
 */
class ScreenshotRule<A : ComponentActivity>(val variant: ScreenshotVariant, activityClass: Class<A>) : TestRule {
    val composeRule: AndroidComposeTestRule<ActivityScenarioRule<A>, A> = createAndroidComposeRule(activityClass)

    private val qualifiers = object : ExternalResource() {
        override fun before() = RuntimeEnvironment.setQualifiers(variant.qualifiers)
    }

    override fun apply(base: Statement, description: Description): Statement =
        RuleChain.outerRule(qualifiers).around(composeRule).apply(base, description)

    /** Renders [content] and records/verifies the golden for this variant (see the class KDoc for the path). */
    fun capture(name: String, content: @Composable (darkTheme: Boolean) -> Unit) {
        render(content)
        drawContent().captureRoboImage(goldenPath(name))
    }

    internal fun goldenPath(name: String): String {
        val language = composeRule.activity.resources.configuration.locales[0].language
        val localePart = if (language == "en") "" else "_$language"
        return "src/test/screenshots/$name${localePart}_$variant.png"
    }

    /** Sets [content] once per test (a second call fails) and lets composition, layout and a frame finish. */
    fun render(content: @Composable (darkTheme: Boolean) -> Unit) {
        composeRule.setContent { content(variant.darkTheme) }
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

    companion object {
        private const val FRAME_SETTLE_MILLIS = 100L

        /** A rule hosted in a plain `ComponentActivity`. */
        operator fun invoke(variant: ScreenshotVariant): ScreenshotRule<ComponentActivity> =
            ScreenshotRule(variant, ComponentActivity::class.java)
    }
}
