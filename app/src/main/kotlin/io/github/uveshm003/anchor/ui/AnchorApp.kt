package io.github.uveshm003.anchor.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.uveshm003.anchor.core.designsystem.theme.AnchorTheme
import io.github.uveshm003.anchor.feature.anchors.navigation.anchorsScreen
import io.github.uveshm003.anchor.feature.home.navigation.HomeRoute
import io.github.uveshm003.anchor.feature.home.navigation.homeScreen
import io.github.uveshm003.anchor.feature.home.navigation.navigateToEmergencyUnlock
import io.github.uveshm003.anchor.feature.modes.navigation.ModeEditorRoute
import io.github.uveshm003.anchor.feature.modes.navigation.modesScreen
import io.github.uveshm003.anchor.feature.settings.navigation.settingsScreen

/**
 * App shell: top-level destinations in a [NavigationSuiteScaffold], which shows a bottom bar on compact widths
 * and a navigation rail on medium and expanded widths (window size class from `currentWindowAdaptiveInfo`).
 */
@Composable
fun AnchorApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    openEmergencyUnlock: Boolean = false,
    onOpenHandle: () -> Unit = {},
    graph: NavGraphBuilder.(NavHostController) -> Unit = { anchorGraph(it) },
) {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    val currentOnOpenHandled by rememberUpdatedState(onOpenHandle)
    LaunchedEffect(openEmergencyUnlock) {
        if (openEmergencyUnlock) {
            navController.navigateToEmergencyUnlock()
            currentOnOpenHandled()
        }
    }
    NavigationSuiteScaffold(
        modifier = modifier,
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                item(
                    selected = currentDestination.isOn(destination),
                    onClick = { navController.navigateToTopLevel(destination) },
                    icon = { Icon(painterResource(destination.icon), contentDescription = null) },
                    label = { Text(stringResource(destination.label)) },
                )
            }
        },
    ) {
        NavHost(navController = navController, startDestination = HomeRoute) { graph(navController) }
    }
}

/** The real destinations; tests of the shell pass stand-ins instead. */
fun NavGraphBuilder.anchorGraph(navController: NavHostController) {
    homeScreen(
        navController,
        onCreateMode = { navController.navigate(ModeEditorRoute()) },
        onPairAnchor = { navController.navigateToTopLevel(TopLevelDestination.Anchors) },
    )
    modesScreen(navController)
    anchorsScreen()
    settingsScreen()
}

private fun NavDestination?.isOn(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.route::class) } == true

/** Single top: keep one copy of each top-level destination and restore its state when coming back. */
private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@PreviewLightDark
@Composable
private fun AnchorAppPreview() {
    AnchorTheme(dynamicColor = false) { AnchorApp() }
}
