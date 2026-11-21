package io.github.uveshm003.anchor.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.uveshm003.anchor.core.designsystem.theme.AnchorTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComponentsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun show(content: @Composable () -> Unit) =
        composeRule.setContent { AnchorTheme(dynamicColor = false, content = content) }

    @Test
    fun switchRow_isOneToggleableSwitchForTheWholeRow() {
        var checked by mutableStateOf(false)
        show {
            AnchorListRow(
                title = "Hold notifications",
                supportingText = "Shown when the session ends",
                leadingIcon = AnchorIcons.NotificationsPaused,
                trailing = ListRowTrailing.Switch(checked) { checked = it },
            )
        }

        // One TalkBack stop: the visual Switch must not be a second toggleable node.
        composeRule.onAllNodes(isToggleable()).assertCountEquals(1)
        val row = composeRule.onNodeWithText("Hold notifications")
        row.assertIsOff()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assertHeightIsAtLeast(48.dp)
        row.performClick()

        row.assertIsOn()
        assertEquals(true, checked)
    }

    @Test
    fun disabledSwitchRow_cannotToggle() {
        var checked by mutableStateOf(false)
        show {
            AnchorListRow(
                title = "Places",
                supportingText = "Needs location",
                trailing = ListRowTrailing.Switch(checked) { checked = it },
                enabled = false,
            )
        }

        composeRule.onNodeWithText("Places")
            .assertIsNotEnabled()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .performClick()

        assertEquals(false, checked)
    }

    @Test
    fun chevronRow_isOneButtonThatClicks() {
        var clicks = 0
        show { AnchorListRow(title = "Anchors", trailing = ListRowTrailing.Chevron { clicks++ }) }

        composeRule.onAllNodes(hasClickAction()).assertCountEquals(1)
        composeRule.onNodeWithText("Anchors")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun clickableRow_actsInPlace_andStaticRowIsNotAControl() {
        var clicks = 0
        show {
            Column {
                AnchorListRow(title = "Re-pair anchor", trailing = ListRowTrailing.Clickable { clicks++ })
                AnchorListRow(title = "Version 0.0.1")
            }
        }

        composeRule.onAllNodes(hasClickAction()).assertCountEquals(1)
        composeRule.onNodeWithText("Re-pair anchor").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun sectionHeaderAndTopAppBarTitle_areHeadings() {
        show {
            Column {
                AnchorTopAppBar(title = "Modes")
                SectionHeader("Quiet actions")
            }
        }
        composeRule.onNodeWithText("Modes").assert(isHeading())
        composeRule.onNodeWithText("Quiet actions").assert(isHeading())
    }

    @Test
    fun emptyState_hasAHeadingAndAnOptionalAction() {
        var actions = 0
        show {
            Column {
                EmptyState(
                    icon = AnchorIcons.Tune,
                    title = "No modes yet",
                    body = "Create one, or start from a template.",
                    action = EmptyStateAction("Create mode") { actions++ },
                )
                EmptyState(icon = AnchorIcons.Nfc, title = "No anchors", body = "Pair one from a sticker.")
            }
        }
        composeRule.onNodeWithText("No modes yet").assert(isHeading())
        composeRule.onNodeWithText("Create mode").assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, actions)
        // The second empty state has no action, so there's exactly one button on screen.
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(1)
    }

    @Test
    fun emptyState_scrollsToItsActionOnAShortWindow() {
        show {
            Box(Modifier.height(160.dp)) {
                EmptyState(
                    icon = AnchorIcons.Tune,
                    title = "No modes yet",
                    body = "Create a mode for each part of your day, or start from a template.",
                    action = EmptyStateAction("Create mode") {},
                )
            }
        }
        composeRule.onNodeWithText("Create mode").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun buttons_areAtLeast48dpAndClick() {
        var primary = 0
        var secondary = 0
        show {
            Column {
                AnchorButton("Save", onClick = { primary++ }, leadingIcon = AnchorIcons.Check)
                AnchorOutlinedButton("Cancel", onClick = { secondary++ })
            }
        }
        composeRule.onNodeWithText("Save").assertHeightIsAtLeast(48.dp).performClick()
        composeRule.onNodeWithText("Cancel").assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, primary)
        assertEquals(1, secondary)
    }

    @Test
    fun iconButton_isLabelledAnd48dp() {
        var clicks = 0
        show { AnchorIconButton(AnchorIcons.ArrowBack, contentDescription = "Back", onClick = { clicks++ }) }
        // M3 draws the icon button at 40 dp and extends its touch target to the 48 dp minimum.
        composeRule.onNodeWithContentDescription("Back")
            .assertTouchHeightIsEqualTo(48.dp)
            .assertTouchWidthIsEqualTo(48.dp)
            .performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun modeBadge_exposesItsContentDescription() {
        show { ModeBadge(icon = AnchorIcons.Work, color = Color(0xFF2E7D32), contentDescription = "Deep Work") }
        composeRule.onNodeWithContentDescription("Deep Work").assertIsDisplayed()
        composeRule.onAllNodesWithText("Deep Work").assertCountEquals(0)
    }
}
