package io.github.uveshm003.anchor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.uveshm003.anchor.feature.anchors.R as AnchorsR
import io.github.uveshm003.anchor.feature.anchors.navigation.AnchorsRoute
import io.github.uveshm003.anchor.feature.home.R as HomeR
import io.github.uveshm003.anchor.feature.home.navigation.EmergencyUnlockRoute
import io.github.uveshm003.anchor.feature.home.navigation.HomeRoute
import io.github.uveshm003.anchor.feature.modes.R as ModesR
import io.github.uveshm003.anchor.feature.modes.navigation.ModesRoute
import io.github.uveshm003.anchor.feature.settings.R as SettingsR
import io.github.uveshm003.anchor.feature.settings.navigation.SettingsRoute

/** Stand-in destinations, so shell tests need no Hilt or data. */
internal fun NavGraphBuilder.stubGraph() {
    composable<HomeRoute> { Stub(HomeR.string.feature_home_placeholder) }
    composable<ModesRoute> { Stub(ModesR.string.feature_modes_placeholder) }
    composable<AnchorsRoute> { Stub(AnchorsR.string.feature_anchors_placeholder) }
    composable<SettingsRoute> { Stub(SettingsR.string.feature_settings_placeholder) }
    composable<EmergencyUnlockRoute> { Text("emergency") }
}

@androidx.compose.runtime.Composable
private fun Stub(text: Int) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(stringResource(text)) }
}
