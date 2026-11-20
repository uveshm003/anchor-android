package io.github.uveshm003.anchor.core.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

    private fun show(content: @androidx.compose.runtime.Composable () -> Unit) =
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

        val row = composeRule.onNodeWithText("Hold notifications")
        row.assertIsOff()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assertHeightIsAtLeast(48.dp)
        row.performClick()

        row.assertIsOn()
        assertEquals(true, checked)
        assertEquals(
            ToggleableState.On,
            row.fetchSemanticsNode().config[SemanticsProperties.ToggleableState],
        )
    }

    @Test
    fun chevronRow_isAButtonThatClicks() {
        var clicks = 0
        show { AnchorListRow(title = "Anchors", trailing = ListRowTrailing.Chevron, onClick = { clicks++ }) }

        composeRule.onNodeWithText("Anchors")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun disabledRow_ignoresClicks() {
        var clicks = 0
        show { AnchorListRow(title = "Places", enabled = false, onClick = { clicks++ }) }
        composeRule.onNodeWithText("Places").assertIsNotEnabled().performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun sectionHeaderAndTopAppBarTitle_areHeadings() {
        show {
            androidx.compose.foundation.layout.Column {
                AnchorTopAppBar(title = "Modes")
                SectionHeader("Quiet actions")
            }
        }
        composeRule.onNodeWithText("Modes").assert(isHeading())
        composeRule.onNodeWithText("Quiet actions").assert(isHeading())
    }

    @Test
    fun emptyState_hasAHeadingAndAnAction() {
        var actions = 0
        show {
            EmptyState(
                icon = AnchorIcons.Tune,
                title = "No modes yet",
                body = "Create one, or start from a template.",
                actionText = "Create mode",
                onAction = { actions++ },
            )
        }
        composeRule.onNodeWithText("No modes yet").assert(isHeading())
        composeRule.onNodeWithText("Create mode").assertHeightIsAtLeast(48.dp).performClick()
        assertEquals(1, actions)
    }

    @Test
    fun buttons_areAtLeast48dpAndClick() {
        var primary = 0
        var secondary = 0
        show {
            androidx.compose.foundation.layout.Column {
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
    fun modeBadge_exposesItsContentDescription() {
        show { ModeBadge(icon = AnchorIcons.Work, color = Color(0xFF2E7D32), contentDescription = "Deep Work") }
        composeRule.onNodeWithContentDescription("Deep Work").assertIsDisplayed()
    }
}
