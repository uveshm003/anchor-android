package io.github.uveshm003.anchor.feature.modes.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.uveshm003.anchor.core.data.platform.DeviceSetup
import io.github.uveshm003.anchor.core.data.platform.EssentialApps
import io.github.uveshm003.anchor.core.data.repository.AnchorRepository
import io.github.uveshm003.anchor.core.data.repository.InstalledApp
import io.github.uveshm003.anchor.core.data.repository.InstalledAppsRepository
import io.github.uveshm003.anchor.core.data.repository.ModeRepository
import io.github.uveshm003.anchor.core.data.repository.SettingsRepository
import io.github.uveshm003.anchor.core.engine.Schedules
import io.github.uveshm003.anchor.core.model.Anchor
import io.github.uveshm003.anchor.core.model.AnchorId
import io.github.uveshm003.anchor.core.model.AppPolicy
import io.github.uveshm003.anchor.core.model.ExitRule
import io.github.uveshm003.anchor.core.model.Mode
import io.github.uveshm003.anchor.core.model.ModeColor
import io.github.uveshm003.anchor.core.model.ModeIcon
import io.github.uveshm003.anchor.core.model.ModeId
import io.github.uveshm003.anchor.core.model.QuietActions
import io.github.uveshm003.anchor.core.model.StartTrigger
import io.github.uveshm003.anchor.feature.modes.navigation.ModeEditorRoute
import io.github.uveshm003.anchor.feature.modes.templates.ModeTemplate
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** What the user is editing. Saved in [SavedStateHandle] so it survives process death. */
@Serializable
data class ModeDraft(
    val name: String = "",
    val icon: ModeIcon = ModeIcon.Focus,
    val color: ModeColor = ModeColor.Teal,
    val allowList: Boolean = false,
    val packages: Set<String> = emptySet(),
    val timerMinutes: Int? = null,
    val quietActions: QuietActions = QuietActions(),
    /** With no timer: the anchor that ends it, or null for any anchor. */
    val anchorId: Long? = null,
    /** Starts automatically on this weekly schedule. */
    val schedule: ScheduleDraft? = null,
    /** Ends when its automatic trigger (the schedule) ends. */
    val endsWithTrigger: Boolean = false,
)

/** Days are ISO (1 = Monday); times are minutes after midnight. */
@Serializable
data class ScheduleDraft(
    val days: Set<Int> = WEEKDAYS,
    val startMinute: Int = DEFAULT_START,
    val endMinute: Int = DEFAULT_END,
) {
    val valid: Boolean get() = days.isNotEmpty() && startMinute != endMinute

    fun toTrigger() = StartTrigger.Schedule(days, startMinute, endMinute)

    private companion object {
        const val DEFAULT_START = 9 * 60
        const val DEFAULT_END = 17 * 60

        /** Monday to Friday (ISO day numbers). */
        val WEEKDAYS: Set<Int> = (java.time.DayOfWeek.MONDAY.value..java.time.DayOfWeek.FRIDAY.value).toSet()
    }
}

/** What the editor reads besides the mode itself. */
class EditorSources @Inject constructor(
    val installedApps: InstalledAppsRepository,
    val anchors: AnchorRepository,
    val essentialApps: EssentialApps,
    val clock: Clock,
    val device: DeviceSetup,
)

/** How a session of this mode ends. */
sealed interface ExitChoice {
    data object AnyAnchor : ExitChoice

    data class SpecificAnchor(val anchorId: Long) : ExitChoice

    data class Timer(val minutes: Int) : ExitChoice

    /** Ends when the schedule window closes. */
    data object TriggerEnds : ExitChoice
}

data class ModeEditorUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val draft: ModeDraft = ModeDraft(),
    val nameError: Boolean = false,
    val dirty: Boolean = false,
    val pickingApps: Boolean = false,
    val query: String = "",
    val apps: List<InstalledApp> = emptyList(),
    val essentials: Set<String> = emptySet(),
    val anchors: List<Anchor> = emptyList(),
    val scheduleError: Boolean = false,
    /** When the draft schedule next opens, local time (null without a valid schedule). */
    val nextScheduleStart: LocalDateTime? = null,
    /** False: schedules may run up to 10 minutes late until exact alarms are allowed. */
    val exactAlarms: Boolean = true,
) {
    val shownApps: List<InstalledApp>
        get() = if (query.isBlank()) apps else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
}

sealed interface ModeEditorEffect {
    data object Saved : ModeEditorEffect
}

@HiltViewModel
class ModeEditorViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val modes: ModeRepository,
    private val settings: SettingsRepository,
    private val sources: EditorSources,
) : ViewModel() {
    private val route = savedState.toRoute<ModeEditorRoute>()
    private val modeId = ModeId(route.modeId)
    private val editor = MutableStateFlow(ModeEditorUiState(isNew = modeId.value == 0L))
    private var original = ModeDraft()
    private var existing: Mode? = null

    val uiState: StateFlow<ModeEditorUiState> = combine(editor, sources.installedApps.apps, sources.anchors.anchors) {
            state,
            apps,
            paired,
        ->
        state.copy(
            apps = apps,
            anchors = paired,
            nextScheduleStart = state.draft.schedule?.takeIf { it.valid }?.let(::nextStart)
                ?.atZone(sources.clock.zone)?.toLocalDateTime(),
            exactAlarms = sources.device.exactAlarmsAllowed(),
        )
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), editor.value)

    private val _effects = Channel<ModeEditorEffect>(Channel.BUFFERED)
    val effects: Flow<ModeEditorEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            existing = modes.getModes().find { it.id == modeId }
            original = existing?.toDraft() ?: ModeDraft()
            val restored = savedState.get<String>(DRAFT_KEY)?.let { json.decodeFromString<ModeDraft>(it) }
            val essentials = sources.essentialApps.packages(
                includeDefaultSms = settings.settings.first().allowDefaultSmsApp,
            )
            val template = route.template?.let { name -> ModeTemplate.entries.find { it.name == name } }
                ?.draft(route.name.orEmpty(), sources.installedApps.apps.first(), essentials)
            // The draft first: the form (and its name field) is seeded when loading turns false.
            edit { restored ?: template ?: original }
            editor.update { it.copy(loading = false, essentials = essentials) }
        }
    }

    fun setName(name: String) = edit { it.copy(name = name) }.also { editor.update { s -> s.copy(nameError = false) } }

    fun setLook(icon: ModeIcon? = null, color: ModeColor? = null) =
        edit { it.copy(icon = icon ?: it.icon, color = color ?: it.color) }

    /** Turns the schedule on, edits it, or (null) turns it off, which also drops "ends with its trigger". */
    fun setSchedule(schedule: ScheduleDraft?) = edit {
        it.copy(schedule = schedule, endsWithTrigger = it.endsWithTrigger && schedule != null)
    }

    fun setAllowList(allowList: Boolean) = edit { it.copy(allowList = allowList) }

    fun setExit(exit: ExitChoice) = edit {
        when (exit) {
            ExitChoice.AnyAnchor -> it.copy(timerMinutes = null, anchorId = null, endsWithTrigger = false)

            is ExitChoice.SpecificAnchor -> it.copy(
                timerMinutes = null,
                anchorId = exit.anchorId,
                endsWithTrigger = false,
            )

            is ExitChoice.Timer -> it.copy(timerMinutes = exit.minutes, anchorId = null, endsWithTrigger = false)

            ExitChoice.TriggerEnds -> it.copy(timerMinutes = null, anchorId = null, endsWithTrigger = true)
        }
    }

    fun toggleApp(packageName: String) {
        if (packageName in editor.value.essentials) return
        edit {
            it.copy(
                packages = if (packageName in
                    it.packages
                ) {
                    it.packages - packageName
                } else {
                    it.packages + packageName
                },
            )
        }
    }

    /** Selects every app matching the search (essentials excluded). */
    fun selectAllShown() {
        val shown = uiState.value.shownApps.map { it.packageName }.filterNot { it in editor.value.essentials }
        edit { it.copy(packages = it.packages + shown) }
    }

    /** Shows or hides the app picker; hiding clears the search. */
    fun showAppPicker(show: Boolean) = editor.update { it.copy(pickingApps = show, query = if (show) it.query else "") }

    fun setQuery(query: String) = editor.update { it.copy(query = query) }

    fun save() {
        val draft = editor.value.draft
        val scheduleInvalid = draft.schedule?.valid == false
        if (draft.name.isBlank() || scheduleInvalid) {
            editor.update { it.copy(nameError = draft.name.isBlank(), scheduleError = scheduleInvalid) }
            return
        }
        viewModelScope.launch {
            val all = modes.getModes()
            val packages = draft.packages - editor.value.essentials
            val mode = (existing ?: Mode(ModeId(0), draft.name.trim(), priority = all.size)).copy(
                name = draft.name.trim(),
                icon = draft.icon,
                color = draft.color,
                appPolicy = if (draft.allowList) AppPolicy.AllowList(packages) else AppPolicy.BlockList(packages),
                startTriggers = (existing?.startTriggers ?: DEFAULT_TRIGGERS)
                    .filterNot { it is StartTrigger.Schedule }.toSet() + setOfNotNull(draft.schedule?.toTrigger()),
                exitRule = when {
                    draft.endsWithTrigger && draft.schedule != null -> ExitRule.TriggerEnds
                    draft.timerMinutes != null -> ExitRule.TimerEnds(draft.timerMinutes)
                    else -> ExitRule.RequiresAnchor(draft.anchorId?.let(::AnchorId))
                },
                quietActions = draft.quietActions,
            )
            val id = modes.upsert(mode)
            // The first mode becomes the default an anchor tap starts.
            if (settings.settings.first().defaultModeId == null) settings.setDefaultMode(id)
            savedState.remove<String>(DRAFT_KEY)
            _effects.send(ModeEditorEffect.Saved)
        }
    }

    private fun edit(transform: (ModeDraft) -> ModeDraft) {
        val draft = transform(editor.value.draft)
        savedState[DRAFT_KEY] = json.encodeToString(draft)
        editor.update { it.copy(draft = draft, dirty = draft != original, scheduleError = false) }
    }

    /** The next time this schedule opens, in the user's zone (same rules as the engine). */
    private fun nextStart(schedule: ScheduleDraft): Instant? {
        val now = sources.clock.instant()
        return Schedules.windows(schedule.toTrigger(), now, sources.clock.zone).map {
            it.start
        }.filter { it.isAfter(now) }.minOrNull()
    }

    private companion object {
        const val DRAFT_KEY = "draft"
        const val STOP_TIMEOUT_MS = 5_000L
        val json = Json { ignoreUnknownKeys = true }
        val DEFAULT_TRIGGERS: Set<StartTrigger> = setOf(StartTrigger.Hold, StartTrigger.AnchorTap)
    }
}

private fun Mode.toDraft() = ModeDraft(
    name = name,
    icon = icon,
    color = color,
    allowList = appPolicy is AppPolicy.AllowList,
    packages = appPolicy.packages,
    timerMinutes = (exitRule as? ExitRule.TimerEnds)?.minutes,
    anchorId = (exitRule as? ExitRule.RequiresAnchor)?.anchorId?.value,
    quietActions = quietActions,
    schedule = startTriggers.filterIsInstance<StartTrigger.Schedule>().firstOrNull()
        ?.let { ScheduleDraft(it.days, it.startMinute, it.endMinute) },
    endsWithTrigger = exitRule == ExitRule.TriggerEnds,
)
