package io.github.uveshm003.anchor.feature.modes.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.uveshm003.anchor.core.data.platform.EssentialApps
import io.github.uveshm003.anchor.core.data.repository.InstalledApp
import io.github.uveshm003.anchor.core.data.repository.InstalledAppsRepository
import io.github.uveshm003.anchor.core.data.repository.ModeRepository
import io.github.uveshm003.anchor.core.data.repository.SettingsRepository
import io.github.uveshm003.anchor.core.model.AppPolicy
import io.github.uveshm003.anchor.core.model.ExitRule
import io.github.uveshm003.anchor.core.model.Mode
import io.github.uveshm003.anchor.core.model.ModeColor
import io.github.uveshm003.anchor.core.model.ModeIcon
import io.github.uveshm003.anchor.core.model.ModeId
import io.github.uveshm003.anchor.core.model.QuietActions
import io.github.uveshm003.anchor.feature.modes.navigation.ModeEditorRoute
import io.github.uveshm003.anchor.feature.modes.templates.ModeTemplate
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
)

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
    private val installedApps: InstalledAppsRepository,
    essentialApps: EssentialApps,
) : ViewModel() {
    private val route = savedState.toRoute<ModeEditorRoute>()
    private val modeId = ModeId(route.modeId)
    private val editor = MutableStateFlow(ModeEditorUiState(isNew = modeId.value == 0L))
    private var original = ModeDraft()
    private var existing: Mode? = null

    val uiState: StateFlow<ModeEditorUiState> = combine(editor, installedApps.apps) { state, apps ->
        state.copy(apps = apps)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), editor.value)

    private val _effects = Channel<ModeEditorEffect>(Channel.BUFFERED)
    val effects: Flow<ModeEditorEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            existing = modes.getModes().find { it.id == modeId }
            original = existing?.toDraft() ?: ModeDraft()
            val restored = savedState.get<String>(DRAFT_KEY)?.let { json.decodeFromString<ModeDraft>(it) }
            val essentials = essentialApps.packages(includeDefaultSms = settings.settings.first().allowDefaultSmsApp)
            val template = route.template?.let { name -> ModeTemplate.entries.find { it.name == name } }
                ?.draft(route.name.orEmpty(), installedApps.apps.first(), essentials)
            // The draft first: the form (and its name field) is seeded when loading turns false.
            edit { restored ?: template ?: original }
            editor.update { it.copy(loading = false, essentials = essentials) }
        }
    }

    fun setName(name: String) = edit { it.copy(name = name) }.also { editor.update { s -> s.copy(nameError = false) } }

    fun setIcon(icon: ModeIcon) = edit { it.copy(icon = icon) }

    fun setColor(color: ModeColor) = edit { it.copy(color = color) }

    fun setAllowList(allowList: Boolean) = edit { it.copy(allowList = allowList) }

    fun setTimer(minutes: Int?) = edit { it.copy(timerMinutes = minutes) }

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
        if (draft.name.isBlank()) {
            editor.update { it.copy(nameError = true) }
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
                exitRule = draft.timerMinutes?.let { ExitRule.TimerEnds(it) } ?: ExitRule.RequiresAnchor(),
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
        editor.update { it.copy(draft = draft, dirty = draft != original) }
    }

    private companion object {
        const val DRAFT_KEY = "draft"
        const val STOP_TIMEOUT_MS = 5_000L
        val json = Json { ignoreUnknownKeys = true }
    }
}

private fun Mode.toDraft() = ModeDraft(
    name = name,
    icon = icon,
    color = color,
    allowList = appPolicy is AppPolicy.AllowList,
    packages = appPolicy.packages,
    timerMinutes = (exitRule as? ExitRule.TimerEnds)?.minutes,
    quietActions = quietActions,
)
