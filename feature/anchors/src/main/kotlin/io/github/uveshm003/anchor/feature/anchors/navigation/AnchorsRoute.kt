package io.github.uveshm003.anchor.feature.anchors.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.uveshm003.anchor.feature.anchors.AnchorsRoute as AnchorsScreenRoute
import kotlinx.serialization.Serializable

/** Top-level Anchors destination. */
@Serializable
data object AnchorsRoute

fun NavGraphBuilder.anchorsScreen() {
    composable<AnchorsRoute> { AnchorsScreenRoute() }
}
