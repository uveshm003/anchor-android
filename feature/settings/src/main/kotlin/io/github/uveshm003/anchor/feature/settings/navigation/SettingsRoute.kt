package io.github.uveshm003.anchor.feature.settings.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.uveshm003.anchor.feature.settings.SettingsRoute as SettingsScreenRoute
import kotlinx.serialization.Serializable

/** Top-level Settings destination. */
@Serializable
data object SettingsRoute

fun NavGraphBuilder.settingsScreen() {
    composable<SettingsRoute> { SettingsScreenRoute() }
}
