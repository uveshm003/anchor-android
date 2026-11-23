package io.github.uveshm003.anchor.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.uveshm003.anchor.core.designsystem.component.AnchorIcons
import io.github.uveshm003.anchor.feature.anchors.R as AnchorsR
import io.github.uveshm003.anchor.feature.anchors.navigation.AnchorsRoute
import io.github.uveshm003.anchor.feature.home.R as HomeR
import io.github.uveshm003.anchor.feature.home.navigation.HomeRoute
import io.github.uveshm003.anchor.feature.modes.R as ModesR
import io.github.uveshm003.anchor.feature.modes.navigation.ModesRoute
import io.github.uveshm003.anchor.feature.settings.R as SettingsR
import io.github.uveshm003.anchor.feature.settings.navigation.SettingsRoute

/** Destinations in the navigation bar/rail, in display order. Insights joins in M5. */
enum class TopLevelDestination(val route: Any, @DrawableRes val icon: Int, @StringRes val label: Int) {
    Home(HomeRoute, AnchorIcons.Home, HomeR.string.feature_home_title),
    Modes(ModesRoute, AnchorIcons.Tune, ModesR.string.feature_modes_title),
    Anchors(AnchorsRoute, AnchorIcons.Nfc, AnchorsR.string.feature_anchors_title),
    Settings(SettingsRoute, AnchorIcons.Settings, SettingsR.string.feature_settings_title),
}
