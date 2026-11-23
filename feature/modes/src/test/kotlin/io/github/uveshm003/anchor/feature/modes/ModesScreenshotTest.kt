package io.github.uveshm003.anchor.feature.modes

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import io.github.uveshm003.anchor.core.data.repository.InstalledApp
import io.github.uveshm003.anchor.core.designsystem.theme.AnchorTheme
import io.github.uveshm003.anchor.core.model.AppPolicy
import io.github.uveshm003.anchor.core.model.ExitRule
import io.github.uveshm003.anchor.core.model.Mode
import io.github.uveshm003.anchor.core.model.ModeColor
import io.github.uveshm003.anchor.core.model.ModeIcon
import io.github.uveshm003.anchor.core.model.ModeId
import io.github.uveshm003.anchor.core.screenshottesting.ScreenshotDevice
import io.github.uveshm003.anchor.core.screenshottesting.ScreenshotRule
import io.github.uveshm003.anchor.core.screenshottesting.ScreenshotVariant
import io.github.uveshm003.anchor.feature.modes.editor.AppPickerScreen
import io.github.uveshm003.anchor.feature.modes.editor.ModeDraft
import io.github.uveshm003.anchor.feature.modes.editor.ModeEditorActions
import io.github.uveshm003.anchor.feature.modes.editor.ModeEditorScreen
import io.github.uveshm003.anchor.feature.modes.editor.ModeEditorUiState
import io.github.uveshm003.anchor.feature.modes.list.ModesScreen
import io.github.uveshm003.anchor.feature.modes.list.ModesUiState
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

@RunWith(ParameterizedRobolectricTestRunner::class)
class ModesScreenshotTest(private val variant: ScreenshotVariant) {
    @get:Rule
    val screenshot = ScreenshotRule(variant)

    private val modes = listOf(
        Mode(ModeId(1), "Deep Work", ModeIcon.Focus, ModeColor.Teal, 0, AppPolicy.BlockList(setOf("a", "b", "c"))),
        Mode(
            ModeId(2),
            "Study",
            ModeIcon.Study,
            ModeColor.Indigo,
            1,
            AppPolicy.AllowList(setOf("a")),
            exitRule = ExitRule.TimerEnds(45),
        ),
        Mode(ModeId(3), "Sleep", ModeIcon.Sleep, ModeColor.Purple, 2, AppPolicy.BlockList(setOf("a", "b"))),
    )

    private val editor = ModeEditorUiState(
        loading = false,
        isNew = false,
        draft = ModeDraft(
            "Deep Work",
            ModeIcon.Focus,
            ModeColor.Teal,
            packages = setOf("com.social", "com.video"),
            timerMinutes = 45,
        ),
        essentials = setOf("com.android.dialer"),
        apps = listOf(
            InstalledApp("com.maps", "Maps"),
            InstalledApp("com.android.dialer", "Phone"),
            InstalledApp("com.social", "Social"),
            InstalledApp("com.video", "Video"),
        ),
    )

    private fun capture(name: String, content: @androidx.compose.runtime.Composable () -> Unit) =
        screenshot.capture(name) { dark -> AnchorTheme(darkTheme = dark, dynamicColor = false) { content() } }

    @Test
    fun modesEmpty() = capture("modes_empty") { ModesScreen(ModesUiState(loading = false), {}, {}, {}, {}, {}) }

    @Test
    fun modesList() = capture("modes_list") {
        ModesScreen(ModesUiState(loading = false, modes = modes), {}, {}, {}, {}, {})
    }

    @Test
    fun modeEditor() = capture("mode_editor") { ModeEditorScreen(editor, ModeEditorActions()) }

    @Test
    fun appPicker() = capture("app_picker") { AppPickerScreen(editor.copy(pickingApps = true), {}, {}, {}, {}) }

    @Test
    fun appPicker_fontScale2() {
        assumeTrue(variant.device == ScreenshotDevice.Phone)
        capture("app_picker_fontscale2") {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f)) {
                AppPickerScreen(editor.copy(pickingApps = true), {}, {}, {}, {})
            }
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants() = ScreenshotVariant.parameters()
    }
}
