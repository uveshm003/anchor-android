package io.github.uveshm003.anchor.feature.settings

import io.github.uveshm003.anchor.core.designsystem.theme.AnchorTheme
import io.github.uveshm003.anchor.core.nfc.NfcAvailability
import io.github.uveshm003.anchor.core.screenshottesting.ScreenshotRule
import io.github.uveshm003.anchor.core.screenshottesting.ScreenshotVariant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

@RunWith(ParameterizedRobolectricTestRunner::class)
class SettingsScreenshotTest(variant: ScreenshotVariant) {
    @get:Rule
    val screenshot = ScreenshotRule(variant)

    private fun capture(name: String, state: SetupState) = screenshot.capture(name) { dark ->
        AnchorTheme(darkTheme = dark, dynamicColor = false) { SettingsScreen(state, open = {}) }
    }

    @Test
    fun allGood() = capture("settings_ok", SetupState())

    @Test
    fun needsAttention() = capture(
        "settings_attention",
        SetupState(
            blockerEnabled = false,
            needsRestrictedSettings = true,
            batteryOptimized = true,
            oem = Oem.Xiaomi,
            nfc = NfcAvailability.Disabled,
        ),
    )

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants() = ScreenshotVariant.parameters()
    }
}
