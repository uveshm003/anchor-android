# Anchor

**Tap to focus. Tap again to come back.**

Anchor turns any cheap NFC sticker (or a printed QR code) into a physical "anchor" for your phone.
Tap the phone on it to start a focus mode that blocks the apps you choose; to end the session you
have to walk back and tap the anchor again. Flexible to start, strict to stop.

[![CI](https://github.com/uveshm003/anchor-android/actions/workflows/ci.yml/badge.svg)](https://github.com/uveshm003/anchor-android/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)

> Inspired by the idea behind [Brick](https://getbrick.com). Anchor is an independent open-source
> project, not affiliated with or endorsed by Brick, and uses none of its branding or assets.

> **Status:** early development. See the [roadmap](#roadmap); nothing below is usable yet unless the
> roadmap marks it done.

## Features (planned)

- **Anchors**: pair NFC tags (NTAG213/215/216) or printable QR codes. Several per phone: desk,
  bedroom, car.
- **Modes**: Deep Work, Study, Sleep, Driving, Family Time or your own. Block a list of apps, or
  allow only a list. Phone, emergency calls, the launcher and Anchor itself are never blocked.
- **Start however you like**: press and hold in the app, tap an anchor, Quick Settings tile,
  widget, app shortcut, or automatically on a schedule, at a place, while driving, when a Bluetooth
  device connects, or when you walk into a BLE "Space" such as your bedroom.
- **At your desk or PC, with nothing installed on it**: when the phone is Bluetooth-connected to your
  computer (Windows keeps a link through Phone Link; on macOS and Linux it works while a Bluetooth
  connection is open), near a tiny Bluetooth beacon on your desk, connected to a chosen Wi-Fi network or
  access point, or plugged into the computer by USB.
- **Stop deliberately**: by tapping your anchor, when a timer ends, or when the trigger that started
  the mode ends (you leave the place, the car disconnects).
- **Quiet actions**: Do Not Disturb, ringer mode, hold notifications from blocked apps until the
  session ends ("While you were anchored"), optional driving auto-reply.
- **Safety first**: 3 emergency unlocks per rolling 7 days, and a 24-hour delayed unlock if an
  anchor is lost. Nobody gets locked out for good.
- **Strict mode** (opt-in): extra friction against uninstalling or changing settings mid-session.
- **Weekly Activity**: time anchored, sessions per mode, block attempts, streaks.
- English and Hindi.

## Privacy

Anchor works entirely on your device. It has **no `INTERNET` permission**, and CI fails the build
if any dependency tries to add one. No accounts, analytics, crash reporting, ads or backend.
Anchor stores only a salted hash of each tag's ID, and held notifications are stored as
package + count + time unless you opt in to keeping titles.

## Roadmap

| Milestone | Goal | Release | Status |
|---|---|---|---|
| M0 Foundation | Design system, app shell, test infrastructure, architecture docs | – | In progress |
| M1 Modes & blocking core | Modes, engine v1, blocker service, hold-to-start, emergency unlock, onboarding | `v0.1.0` | Planned |
| M2 Anchors | NFC + QR anchors, tap to start/stop, lost-anchor recovery | `v0.2.0` | Planned |
| M3 Automatic modes | Schedule, driving, Bluetooth, Spaces, places, Wi-Fi, USB, conflict resolution | `v0.3.0` | Planned |
| M4 Quiet actions | DND, ringer, notification holding, driving auto-reply | `v0.4.0` | Planned |
| M5 Strict mode, insights & surfaces | Strict guards, Weekly Activity, widget, tile, shortcuts, Hindi | `v0.5.0` | Planned |
| M6 Hardening & release | Resilience, battery audit, baseline profile, Play readiness | `v1.0.0` | Planned |

Progress is tracked in [issues](https://github.com/uveshm003/anchor-android/issues) and
[milestones](https://github.com/uveshm003/anchor-android/milestones).

## Building

Requirements: JDK 21 or newer (CI uses 25), Android SDK with platform 37, and an Android 12+ device
or emulator. NFC needs a real phone.

```bash
./gradlew assembleDebug            # build the debug APK
./gradlew installDebug             # install on a connected device
./gradlew spotlessApply ciCheck    # everything CI runs: format, detekt, lint, tests, screenshots, coverage, privacy
./gradlew recordRoborazziDebug     # re-record screenshot goldens after an intended UI change
./gradlew connectedDebugAndroidTest  # instrumented tests on a device
```

## Architecture

Single-activity Jetpack Compose app, unidirectional data flow, and a pure-Kotlin **mode engine**
that turns trigger events and user intents into a new session state plus a list of commands that thin
Android adapters execute. Details: [`docs/architecture.md`](docs/architecture.md), decisions:
[`docs/adr/`](docs/adr/).

<!-- module-graph:start -->
```mermaid
graph TD
    app[app]
    baselineprofile[baselineprofile]
    benchmark[benchmark]
    widget[widget]
    subgraph core
        core_common[common]
        core_data[data]
        core_database[database]
        core_datastore[datastore]
        core_designsystem[designsystem]
        core_engine[engine]
        core_model[model]
        core_nfc[nfc]
        core_qr[qr]
        core_screenshot_testing[screenshot-testing]
        core_testing[testing]
        core_triggers[triggers]
        core_ui[ui]
    end
    subgraph feature
        feature_anchors[anchors]
        feature_home[home]
        feature_insights[insights]
        feature_modes[modes]
        feature_onboarding[onboarding]
        feature_settings[settings]
        feature_triggers[triggers]
    end
    subgraph service
        service_blocker[blocker]
        service_notifications[notifications]
    end
    app --> core_data
    app --> core_designsystem
    app --> core_ui
    app --> feature_anchors
    app --> feature_home
    app --> feature_insights
    app --> feature_modes
    app --> feature_onboarding
    app --> feature_settings
    app --> feature_triggers
    app --> service_blocker
    app --> service_notifications
    app --> widget
    core_data --> core_common
    core_data --> core_database
    core_data --> core_datastore
    core_data --> core_engine
    core_data --> core_model
    core_database --> core_model
    core_datastore --> core_common
    core_datastore --> core_model
    core_engine --> core_common
    core_engine --> core_model
    core_nfc --> core_common
    core_nfc --> core_engine
    core_qr --> core_common
    core_qr --> core_engine
    core_testing --> core_common
    core_testing --> core_model
    core_triggers --> core_common
    core_triggers --> core_engine
    core_ui --> core_designsystem
    core_ui --> core_model
    feature_anchors --> core_data
    feature_anchors --> core_designsystem
    feature_anchors --> core_ui
    feature_home --> core_data
    feature_home --> core_designsystem
    feature_home --> core_ui
    feature_insights --> core_data
    feature_insights --> core_designsystem
    feature_insights --> core_ui
    feature_modes --> core_data
    feature_modes --> core_designsystem
    feature_modes --> core_ui
    feature_onboarding --> core_data
    feature_onboarding --> core_designsystem
    feature_onboarding --> core_ui
    feature_settings --> core_data
    feature_settings --> core_designsystem
    feature_settings --> core_ui
    feature_triggers --> core_data
    feature_triggers --> core_designsystem
    feature_triggers --> core_ui
    service_blocker --> core_common
    service_blocker --> core_data
    service_notifications --> core_common
    service_notifications --> core_data
    widget --> core_data
    widget --> core_designsystem
```
<!-- module-graph:end -->

## Contributing

Every change goes through an issue and a reviewed pull request. See
[CONTRIBUTING.md](CONTRIBUTING.md). Security reports: [SECURITY.md](SECURITY.md).

## License

[Apache License 2.0](LICENSE)
