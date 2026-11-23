package io.github.uveshm003.anchor.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpRect
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.uveshm003.anchor.core.designsystem.theme.AnchorTheme
import io.github.uveshm003.anchor.feature.anchors.R as AnchorsR
import io.github.uveshm003.anchor.feature.home.R as HomeR
import io.github.uveshm003.anchor.feature.settings.R as SettingsR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class AnchorAppNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun text(id: Int) = composeRule.activity.getString(id)

    @Test
    fun navigates_between_top_level_destinations_and_back_returns_home() {
        composeRule.setContent { AnchorTheme(dynamicColor = false) { AnchorApp() } }
        composeRule.onNodeWithText(text(HomeR.string.feature_home_placeholder)).assertIsDisplayed()

        composeRule.onNodeWithText(text(AnchorsR.string.feature_anchors_title), useUnmergedTree = true).performClick()
        composeRule.onNodeWithText(text(AnchorsR.string.feature_anchors_placeholder)).assertIsDisplayed()

        pressBack()
        composeRule.onNodeWithText(text(HomeR.string.feature_home_placeholder)).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun compact_width_uses_a_bottom_bar() {
        val settings = settingsItemBounds()

        assertThat(settings.top.value).isGreaterThan(SCREEN_HEIGHT_COMPACT / 2f)
    }

    @Test
    @Config(qualifiers = "w700dp-h900dp")
    fun medium_width_uses_a_navigation_rail() {
        val settings = settingsItemBounds()

        assertThat(settings.left.value).isLessThan(RAIL_MAX_WIDTH)
        assertThat(settings.top.value).isLessThan(SCREEN_HEIGHT_MEDIUM / 2f)
    }

    private fun settingsItemBounds(): DpRect {
        composeRule.setContent { AnchorTheme(dynamicColor = false) { AnchorApp() } }
        return composeRule.onNodeWithText(text(SettingsR.string.feature_settings_title), useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
    }

    private companion object {
        const val SCREEN_HEIGHT_COMPACT = 891f
        const val SCREEN_HEIGHT_MEDIUM = 900f
        const val RAIL_MAX_WIDTH = 120f
    }
}
