package io.github.uveshm003.anchor.feature.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.uveshm003.anchor.feature.home.HomeScreen
import kotlinx.serialization.Serializable

/** Top-level Home destination. */
@Serializable
data object HomeRoute

fun NavGraphBuilder.homeScreen() {
    composable<HomeRoute> { HomeScreen() }
}
