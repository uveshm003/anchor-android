# 2. State management and unidirectional data flow

- **Status:** Accepted
- **Date:** 2026-11-19

## Context

Anchor's screens show state that changes from many directions at once: a session ticking, a trigger firing
in the background, a permission being revoked in Settings. Brief §3 requires unidirectional data flow, no
business logic in composables, screenshot tests for every screen, and survival of process death. We need
one pattern every feature follows so reviews can check it mechanically.

## Decision

**ViewModel contract**

- Each screen has one ViewModel exposing exactly one `val uiState: StateFlow<XUiState>`.
- `XUiState` is an immutable data class (or sealed interface for distinct phases such as
  `Loading`/`Ready`/`Error`). It holds display-ready values; formatting that depends on resources happens
  in the UI.
- State is derived with `combine(...)`/`map` from repository flows and
  `.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)`. The 5 s keeps work alive
  across configuration changes without leaking it when the user leaves.
- User actions are plain functions on the ViewModel (`fun onModeSelected(id: ModeId)`), never a public
  `MutableStateFlow` and never a generic `onEvent(Event)` dispatcher.
- **One-off effects** (navigate, show a snackbar, launch a system intent) go through a private
  `Channel<XEffect>(Channel.BUFFERED)` exposed as `val effects: Flow<XEffect> = channel.receiveAsFlow()`.
  The UI collects it with `LaunchedEffect` on the lifecycle (`repeatOnLifecycle(STARTED)`), so effects are
  delivered once and not lost while the UI is stopped.
- Input that must survive process death (form fields, the selected item) lives in `SavedStateHandle`.
  Everything else is re-derived from repositories.
- Dispatchers and `Clock` are injected (`@Dispatcher`, `java.time.Clock`); ViewModel tests use
  `MainDispatcherRule` and `TestClock`.

**Composables**

- Each screen is split into a *route* (`XRoute`: gets the ViewModel, collects state/effects) and a
  stateless *screen* (`XScreen(uiState, onAction…, modifier)`). Only the stateless screen is used in
  previews and screenshot tests (`core:screenshot-testing`).
- Composables hold only UI state (scroll, expanded, focus). Anything a test would want to assert on
  belongs in the ViewModel.

**Services and receivers** follow the same rule from the other side: they translate platform signals into
calls on a repository or the session controller and contain no decisions (ADR 0003).

## Consequences

- Every screen is screenshot-testable with fake state and ViewModel-testable with Turbine on one flow.
- Reviews can check a short list: one `StateFlow`, functions for actions, `Channel` for effects, no logic in
  composables.
- Slight boilerplate per screen (route + screen + state class). Accepted for testability.

## Alternatives considered

- **MVI with a single `onEvent(Event)` reducer in the ViewModel:** more ceremony, and the reducer pattern
  is reserved for the domain where it pays off (the mode engine, ADR 0003).
- **`SharedFlow` for effects:** drops events with no active collector unless replay is configured, and
  replay re-delivers them after configuration changes. A buffered `Channel` delivers each once.
- **Compose state (`mutableStateOf`) in ViewModels:** couples ViewModels to the Compose runtime and is
  harder to test with Turbine.
