package io.github.uveshm003.anchor.feature.home.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.uveshm003.anchor.feature.home.HomeRoute as HomeScreenRoute
import io.github.uveshm003.anchor.feature.home.emergency.EmergencyUnlockRoute as EmergencyUnlockScreenRoute
import kotlinx.serialization.Serializable

/** Top-level Home destination. */
@Serializable
data object HomeRoute

@Serializable
data object EmergencyUnlockRoute

fun NavController.navigateToEmergencyUnlock() = navigate(EmergencyUnlockRoute) { launchSingleTop = true }

/** [onCreateMode] and [onPairAnchor] are wired by the app (features don't depend on each other). */
fun NavGraphBuilder.homeScreen(navController: NavController, onCreateMode: () -> Unit, onPairAnchor: () -> Unit) {
    composable<HomeRoute> {
        HomeScreenRoute(
            onCreateMode = onCreateMode,
            onEmergencyUnlock = navController::navigateToEmergencyUnlock,
            onPairAnchor = onPairAnchor,
        )
    }
    composable<EmergencyUnlockRoute> { EmergencyUnlockScreenRoute(onClose = { navController.popBackStack() }) }
}
