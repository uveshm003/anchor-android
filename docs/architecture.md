# Architecture

Anchor is a single-activity Jetpack Compose app split into Gradle modules by layer and feature.
This page is the overview; decisions and their reasons live in [`adr/`](adr/), and the product brief
is in [`PROJECT_BRIEF.md`](PROJECT_BRIEF.md).

## Layers

```
UI (Compose screens + ViewModels)      feature/*, widget, core/ui, core/designsystem
        │  one StateFlow<UiState> per ViewModel, actions are functions, effects via Channel
        ▼
Domain (use cases, only where they add value) + the mode engine            core/engine
        │  pure Kotlin: (SessionState, Input) -> (SessionState, List<Command>)
        ▼
Data (repositories)                                                        core/data
        │
        ▼
Sources: Room, DataStore, platform adapters      core/database, core/datastore,
                                                 core/nfc, core/qr, core/triggers,
                                                 service/blocker, service/notifications
```

Rules:

- **Unidirectional data flow.** State flows down, events flow up. Composables are stateless where
  possible and contain no business logic.
- **The engine decides, adapters execute.** Platform code (receivers, services, NFC callbacks) only
  converts a platform signal into an engine input, and executes the commands the engine returns.
  No decisions in services or receivers.
- **Persist before side effects.** The new `SessionState` is written before any command runs, so a
  process death mid-way can be recovered.
- **Inject everything that varies:** dispatchers, `Clock`, platform managers. Tests use fakes.
- **Features never depend on other features.** Navigation between them is wired in `app`.

## Modules

| Module | Kind | Responsibility |
|---|---|---|
| `app` | Android app | Activity, navigation graph, DI root, wiring of features |
| `core:model` | Kotlin/JVM | Domain types: Mode, Session, Anchor, Place, Space, … |
| `core:common` | Kotlin/JVM | Coroutine dispatchers, result types, small utilities |
| `core:engine` | Kotlin/JVM | The mode engine state machine (≥ 90 % coverage) |
| `core:database` | Android lib | Room database, DAOs, migrations |
| `core:datastore` | Android lib | DataStore settings |
| `core:data` | Android lib | Repositories; the only entry point for features into data |
| `core:designsystem` | Compose lib | Theme, typography, core components |
| `core:ui` | Compose lib | Shared UI built on the design system and the model |
| `core:testing` | Android lib | Test rules, fakes, fake `Clock`, screenshot helpers |
| `core:nfc` | Android lib | NFC reader mode, NDEF + AAR writing, UID hashing |
| `core:qr` | Android lib | QR generation and scanning |
| `core:triggers` | Android lib | Schedule, driving, Bluetooth, Space and place triggers |
| `service:blocker` | Android lib | AccessibilityService: foreground detection, block screen |
| `service:notifications` | Android lib | NotificationListenerService: holding and summary |
| `feature:*` | Compose lib | Screens + ViewModels: onboarding, home, modes, anchors, triggers, insights, settings |
| `widget` | Compose lib | Glance widget, Quick Settings tile, app shortcuts |
| `baselineprofile`, `benchmark` | Android test | Baseline Profile generation and Macrobenchmarks |

Build configuration lives in `build-logic/` convention plugins (`anchor.android.library`,
`anchor.android.feature`, `anchor.jvm.library`, …) so module build files only declare dependencies.

## Quality gates

`./gradlew ciCheck` runs everything CI runs: Spotless (ktlint + Compose rules), Detekt, Android Lint
(errors fail), unit and Robolectric tests, Roborazzi screenshot verification, Kover coverage gates
(≥ 90 % `core:engine`, ≥ 80 % other pure-Kotlin modules, ≥ 60 % overall) and the privacy check that
fails if the merged release manifest requests `android.permission.INTERNET`.
