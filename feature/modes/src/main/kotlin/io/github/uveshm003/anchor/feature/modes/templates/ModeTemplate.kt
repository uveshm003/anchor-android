package io.github.uveshm003.anchor.feature.modes.templates

import androidx.annotation.StringRes
import io.github.uveshm003.anchor.core.data.repository.AppCategory
import io.github.uveshm003.anchor.core.data.repository.InstalledApp
import io.github.uveshm003.anchor.core.model.ModeColor
import io.github.uveshm003.anchor.core.model.ModeIcon
import io.github.uveshm003.anchor.core.model.QuietActions
import io.github.uveshm003.anchor.core.model.RingerMode
import io.github.uveshm003.anchor.feature.modes.R
import io.github.uveshm003.anchor.feature.modes.editor.ModeDraft

/** Starting points for a new mode. Code, not database rows; nothing is saved until the user saves the editor. */
enum class ModeTemplate(
    @StringRes val title: Int,
    @StringRes val description: Int,
    val look: TemplateLook,
    val apps: SuggestedApps,
    val timerMinutes: Int?,
    val quietActions: QuietActions,
) {
    DeepWork(
        R.string.feature_modes_template_deep_work,
        R.string.feature_modes_template_deep_work_body,
        TemplateLook(ModeIcon.Focus, ModeColor.Teal),
        apps = SuggestedApps(
            allowList = false,
            categories = setOf(AppCategory.Social, AppCategory.Game, AppCategory.Video, AppCategory.News),
        ),
        timerMinutes = null,
        quietActions = QuietActions(doNotDisturb = true),
    ),
    Study(
        R.string.feature_modes_template_study,
        R.string.feature_modes_template_study_body,
        TemplateLook(ModeIcon.Study, ModeColor.Indigo),
        apps = SuggestedApps(
            allowList = false,
            categories = setOf(AppCategory.Social, AppCategory.Game, AppCategory.Video),
        ),
        timerMinutes = 45,
        quietActions = QuietActions(doNotDisturb = true),
    ),
    Sleep(
        R.string.feature_modes_template_sleep,
        R.string.feature_modes_template_sleep_body,
        TemplateLook(ModeIcon.Sleep, ModeColor.Purple),
        apps = SuggestedApps(
            allowList = false,
            categories = setOf(AppCategory.Social, AppCategory.Game, AppCategory.Video, AppCategory.News),
        ),
        timerMinutes = null,
        quietActions = QuietActions(doNotDisturb = true, ringer = RingerMode.Silent),
    ),
    Driving(
        R.string.feature_modes_template_driving,
        R.string.feature_modes_template_driving_body,
        TemplateLook(ModeIcon.Driving, ModeColor.Amber),
        apps = SuggestedApps(allowList = true, categories = setOf(AppCategory.Maps, AppCategory.Audio)),
        timerMinutes = null,
        quietActions = QuietActions(doNotDisturb = true),
    ),
    FamilyTime(
        R.string.feature_modes_template_family,
        R.string.feature_modes_template_family_body,
        TemplateLook(ModeIcon.Family, ModeColor.Rose),
        apps = SuggestedApps(
            allowList = false,
            categories = setOf(AppCategory.Social, AppCategory.Game, AppCategory.Video, AppCategory.News),
        ),
        timerMinutes = 120,
        quietActions = QuietActions(ringer = RingerMode.Vibrate),
    ),
    ;

    /** The editor draft for this template, with [name] localised by the caller and apps suggested by category. */
    fun draft(name: String, apps: List<InstalledApp>, essentials: Set<String>) = ModeDraft(
        name = name,
        icon = look.icon,
        color = look.color,
        allowList = this.apps.allowList,
        packages = apps.filter { it.category in this.apps.categories }.map { it.packageName }.toSet() - essentials,
        timerMinutes = timerMinutes,
        quietActions = quietActions,
    )
}

/** Block these app categories, or for an allow list, allow only these. */
data class SuggestedApps(val allowList: Boolean, val categories: Set<AppCategory>)

data class TemplateLook(val icon: ModeIcon, val color: ModeColor)
