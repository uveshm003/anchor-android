package io.github.uveshm003.anchor.core.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import io.github.uveshm003.anchor.core.designsystem.component.AnchorIcons
import io.github.uveshm003.anchor.core.model.ModeColor
import io.github.uveshm003.anchor.core.model.ModeIcon

/** Icon for a mode (fixed set). */
@get:DrawableRes
val ModeIcon.drawable: Int
    get() = when (this) {
        ModeIcon.Focus -> AnchorIcons.Work
        ModeIcon.Study -> AnchorIcons.School
        ModeIcon.Sleep -> AnchorIcons.Bedtime
        ModeIcon.Driving -> AnchorIcons.DirectionsCar
        ModeIcon.Family -> AnchorIcons.FamilyRestroom
        ModeIcon.Timer -> AnchorIcons.Timer
    }

@get:StringRes
val ModeIcon.label: Int
    get() = when (this) {
        ModeIcon.Focus -> R.string.core_ui_icon_focus
        ModeIcon.Study -> R.string.core_ui_icon_study
        ModeIcon.Sleep -> R.string.core_ui_icon_sleep
        ModeIcon.Driving -> R.string.core_ui_icon_driving
        ModeIcon.Family -> R.string.core_ui_icon_family
        ModeIcon.Timer -> R.string.core_ui_icon_timer
    }

/** Mode palette: mid tones that keep a white icon readable (≥ 3:1) in light and dark themes. */
val ModeColor.color: Color
    get() = when (this) {
        ModeColor.Teal -> Color(0xFF00796B)
        ModeColor.Indigo -> Color(0xFF3949AB)
        ModeColor.Purple -> Color(0xFF7B1FA2)
        ModeColor.Rose -> Color(0xFFC2185B)
        ModeColor.Amber -> Color(0xFFB26A00)
        ModeColor.Green -> Color(0xFF2E7D32)
    }

@get:StringRes
val ModeColor.label: Int
    get() = when (this) {
        ModeColor.Teal -> R.string.core_ui_color_teal
        ModeColor.Indigo -> R.string.core_ui_color_indigo
        ModeColor.Purple -> R.string.core_ui_color_purple
        ModeColor.Rose -> R.string.core_ui_color_rose
        ModeColor.Amber -> R.string.core_ui_color_amber
        ModeColor.Green -> R.string.core_ui_color_green
    }
