package io.github.uveshm003.anchor.feature.anchors

import io.github.uveshm003.anchor.core.designsystem.theme.AnchorTheme
import io.github.uveshm003.anchor.core.model.Anchor
import io.github.uveshm003.anchor.core.model.AnchorId
import io.github.uveshm003.anchor.core.model.AnchorType
import io.github.uveshm003.anchor.core.nfc.NfcAvailability
import io.github.uveshm003.anchor.core.screenshottesting.ScreenshotRule
import io.github.uveshm003.anchor.core.screenshottesting.ScreenshotVariant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

/** Pairing dialogs open in their own window (not captured here); AnchorsViewModelTest covers pairing. */
@RunWith(ParameterizedRobolectricTestRunner::class)
class AnchorsScreenshotTest(variant: ScreenshotVariant) {
    @get:Rule
    val screenshot = ScreenshotRule(variant)

    private val anchors = listOf(
        Anchor(AnchorId(1), "Desk", AnchorType.Nfc, "a".repeat(64)),
        Anchor(AnchorId(2), "Bedroom", AnchorType.Nfc, "b".repeat(64)),
        Anchor(AnchorId(3), "Kitchen", AnchorType.Qr, "c".repeat(64)),
    )

    private fun capture(name: String, state: AnchorsUiState) = screenshot.capture(name) { dark ->
        AnchorTheme(darkTheme = dark, dynamicColor = false) { AnchorsScreen(state, AnchorsActions()) }
    }

    @Test
    fun empty() = capture("anchors_empty", AnchorsUiState(loading = false))

    @Test
    fun list() = capture("anchors_list", AnchorsUiState(loading = false, anchors = anchors))

    @Test
    fun nfcOff() = capture("anchors_nfc_off", AnchorsUiState(loading = false, nfc = NfcAvailability.Disabled))

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants() = ScreenshotVariant.parameters()
    }
}
