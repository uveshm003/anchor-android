package io.github.uveshm003.anchor.feature.modes.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.uveshm003.anchor.feature.modes.ModesScreen
import kotlinx.serialization.Serializable

/** Top-level Modes destination. */
@Serializable
data object ModesRoute

fun NavGraphBuilder.modesScreen() {
    composable<ModesRoute> { ModesScreen() }
}
