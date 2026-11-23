package io.github.uveshm003.anchor.feature.modes.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.uveshm003.anchor.feature.modes.editor.ModeEditorRoute as ModeEditorScreenRoute
import io.github.uveshm003.anchor.feature.modes.list.ModesRoute as ModesScreenRoute
import kotlinx.serialization.Serializable

/** Top-level Modes destination. */
@Serializable
data object ModesRoute

/** Create (`modeId = 0`) or edit a mode. */
@Serializable
data class ModeEditorRoute(val modeId: Long = 0)

fun NavGraphBuilder.modesScreen(navController: NavController) {
    composable<ModesRoute> { ModesScreenRoute(onEditMode = { navController.navigate(ModeEditorRoute(it)) }) }
    composable<ModeEditorRoute> { ModeEditorScreenRoute(onDone = { navController.popBackStack() }) }
}
