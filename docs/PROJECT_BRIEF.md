# Build "Anchor": tap-to-focus phone modes for Android (inspired by Brick)

You are the only engineer on this project, working on your own in Claude Code. You will:

1. Create the Android project from scratch.
2. Put it on GitHub and set up everything a professional repo needs (CI, protection, templates, labels, milestones, backlog).
3. Build the app through a strict loop: **Open Issue → Raise PR → Review Code → Merge PR → Repeat**, milestone by milestone, until `v1.0.0`.

Read this whole brief before doing anything. In the first commit, save it unchanged as `docs/PROJECT_BRIEF.md`. It is the source of truth for every future session.

---

## 0. Project settings (I may override these)

| Setting | Value |
|---|---|
| App name | **Anchor** (working name) |
| Repo | `anchor-android`, **public**, under the account `gh` is logged in as (`gh api user -q .login`) |
| applicationId / namespace root | `io.github.<login>.anchor`, with the login lowercased and any character that isn't valid in a package name replaced by `_` |
| License | Apache-2.0 |
| minSdk | 31 (Android 12): modern Bluetooth permission model, CompanionDeviceManager presence APIs, and parity with Brick's own Android minimum |
| compileSdk / targetSdk | Latest stable API level. Check this at setup time (it was Android 17 / API 37 when this brief was written) |
| UI languages | English + Hindi |

---

## 1. Product

### Inspiration: Brick (https://getbrick.com). Read the site once before writing the backlog.
Brick is a $59 physical device. You tap your phone on it to block apps, and blocking can only be ended by tapping it again. Its key ideas:
- **Physical separation.** Leaving focus requires walking to the device, which builds intention, unlike software limits that are easy to skip.
- **"Flexible to start, strict to stop."** Start from the app (press and hold), from a schedule or with a tap. Stopping needs the physical tap.
- **Modes**: custom modes for parts of the day, each choosing what to block and what to keep.
- **Schedules**: blocking that starts automatically at set times.
- **Strict Mode**: blocks deleting the app or changing settings to escape.
- **Brick Night**: a Bluetooth-defined "Space" (e.g. a bedroom) that blocks apps automatically when you enter it.
- **Weekly Activity**: time reclaimed, and progress over time.

**Brick is inspiration only.** Do not use its name, logo, branding, marketing copy, visuals or assets anywhere.

### What Anchor does differently
- The "brick" is **any cheap NFC sticker** (NTAG213/215/216), or a **printed QR code** for phones without NFC. One phone can have several anchors (desk, bedroom, car).
- It adds **automatic context modes**: schedules, places, driving, Bluetooth devices and BLE "Spaces".
- Its **quiet actions** go beyond blocking: Do Not Disturb, ringer control, holding notifications from blocked apps until the session ends, and an optional driving auto-reply.

### Principles
- **Flexible to start, strict to stop.**
- **Safe by design.** Never block the dialer, emergency calls/SOS, system UI, the launcher or Anchor itself. There is **always** a recovery path, so a lost anchor can never lock someone out forever.
- **On-device only.** The merged manifest has **no `INTERNET` permission**. No analytics, crash SDKs, accounts or backend.
- **Honest friction, not a jail.** Strict mode adds friction; it doesn't pretend Android can't be escaped (safe mode, etc.).
- **Battery-friendly.** Event-driven only: no polling, and no always-on foreground service unless an ADR justifies it.
- **Progressive permissions.** Ask for a permission only when the user turns on the feature that needs it.

### Core concepts (domain model)
- **Mode**: name, icon, colour, priority, plus:
  - **App policy**: *Block list* (these apps are blocked) or *Allow list* (only these apps work). **Essentials** are always allowed: phone/dialer, emergency, Anchor, system UI, launcher, and the default SMS app (configurable, default allowed).
  - **Quiet actions**: DND on/off, ringer (normal/vibrate/silent), hold notifications from blocked apps, driving auto-reply (opt-in).
  - **Start triggers** (any combination): hold-to-start, anchor tap, schedule, place, driving, Bluetooth device, Space.
  - **Exit rule**: requires anchor tap (any anchor / a specific anchor), timer end, or "ends when its trigger ends" (leave the place, disconnect the car, etc.).
  - **Strict** flag (opt-in, comes with a clear warning).
- **Session**: one active session at a time. Records mode, start/end time, what started it, what ended it, block attempts, and whether an emergency unlock was used.
- **Anchor**: a paired NFC tag or QR code, with a name. Only a salted hash of the tag UID / QR secret is stored.
- **Place**: a geofence (lat/lng + radius) saved from the current location. No map SDK, which keeps the app free of `INTERNET`.
- **Space**: a BLE device (beacon, ESP32, earbuds case, anything advertising) plus an RSSI threshold with hysteresis. Anchor's equivalent of Brick Night.

### Features
1. **Modes**: create/edit/delete, reorder by priority, and templates (Deep Work, Study, Sleep, Driving, Family Time). The app picker has search, icons, and block-list/allow-list switching.
2. **Start**: hold-to-start in the app (2 s press with haptic progress), anchor tap, Quick Settings tile, app shortcuts, home-screen widget, schedules and automatic triggers.
3. **Stop**: governed by the mode's exit rule. The active-session screen explains how to end the session ("Tap your Desk anchor").
4. **Anchors**:
   - NFC pairing: read the UID. If the tag is writable, write an NDEF record with an app-specific URI **plus an Android Application Record** so a tap opens Anchor even when it isn't running (screen unlocked). Name the anchor.
   - Taps use reader mode while the app is in the foreground and a manifest `NDEF_DISCOVERED` intent filter for background taps. Handle read-only or unsupported tags (UID-only fallback).
   - **QR fallback**: generate a QR anchor, export it as a printable image/PDF via the share sheet, and scan it with CameraX + ML Kit barcode scanning.
5. **Emergency unlock**: a quota of **3 per rolling 7 days** (the quota can only be changed when no session is active). Friction: a 10-second hold plus typing a displayed sentence. Logged in insights.
6. **Lost-anchor recovery**: when the quota is used up, the user can request a **24-hour delayed unlock**, which is cancellable and shown as a countdown. Afterwards, offer to re-pair or replace anchors. Nobody stays locked out permanently.
7. **Blocking**:
   - An AccessibilityService detects the foreground app and shows a block screen with the mode name, elapsed time, "Go home" and the exit options.
   - Check **all windows** (`getWindows()`): split-screen, PiP, bubbles and Android 17 desktop windowing, not just the active one.
   - Apps opened from notifications or shortcuts are caught the same way.
8. **Quiet actions**:
   - DND via `AutomaticZenRule` (use typed rules, e.g. driving/bedtime, where the API level supports them; verify in the docs).
   - Ringer mode.
   - **Notification holding**: a NotificationListenerService removes notifications from blocked apps during a session and stores only package + count + timestamps by default (titles off unless the user opts in). At session end it shows a "While you were anchored" summary.
   - **Driving auto-reply** (stretch, opt-in per app, rate-limited): replies "I'm driving, I'll reply soon" through the notification's RemoteInput action.
9. **Automatic triggers**:
   - **Schedule**: days + time window. Use exact alarms if `SCHEDULE_EXACT_ALARM` is granted; otherwise fall back to a window and tell the user in the UI.
   - **Driving**: Activity Recognition Transition API (enter/exit IN_VEHICLE).
   - **Bluetooth device**: car stereo/earbuds connect and disconnect. Prefer CompanionDeviceManager device-presence observation; fall back to ACL broadcasts. Verify in the docs.
   - **Space**: BLE proximity with RSSI threshold + hysteresis + dwell time. Prefer CompanionDeviceManager presence or `PendingIntent`-based BLE scans over a foreground service. Note that `neverForLocation` filters some beacons; decide in an ADR.
   - **Place**: geofence enter/exit. This needs background location, which has strict Play review, so it is an **optional feature**. Document the policy risk in an ADR.
   - **Calendar busy** (stretch).
   - **Conflict resolution**: one active session at a time. Automatic triggers **never override** a manual/anchor session. When several automatic triggers fire, the highest-priority mode wins. Ending a trigger ends only the session it started, and only if the exit rule allows. All of this lives in the pure engine and is exhaustively tested.
10. **Strict mode** (per mode, opt-in, with a clear warning). While active, the blocker also guards Anchor's App info page, the uninstall dialog for Anchor, Anchor's Accessibility/Notification-access settings and Date & time settings (prevents schedule cheating). Use the same rule-based screen detection as the blocker, with fixtures for stock Android plus the main OEM Settings apps. Emergency unlock and recovery **always** stay available. Write an ADR on the Play-policy risk and **ask me before merging the uninstall-protection part**.
11. **Insights (Weekly Activity)**: time anchored, sessions per mode, block attempts per app, emergency unlocks used, streaks. Charts.
12. **Surfaces**: an ongoing notification while a session is active (mode, elapsed time, how to exit; use Live Updates only if this use case qualifies under the current guidelines), a Glance widget (status + start default mode), a Quick Settings tile (starting is allowed, stopping still follows the exit rule), and app shortcuts per mode.
13. **Onboarding & permission health**:
    - Prominent disclosure + consent before Accessibility (Play policy)
    - Android 13+ restricted-settings guide for sideloaded APKs, battery-optimisation + OEM autostart guidance (link dontkillmyapp.com), and an NFC availability check
    - A **Permission health** screen listing each permission, its status, and which features are degraded without it
14. **Resilience**: re-register alarms, geofences, transitions and presence observations on `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED`. Persist session state **before** running side effects, and restore the active session after process death. Handle `TIME_SET` and `TIMEZONE_CHANGED`.

### Non-goals
No root, no VPN/DNS blocking, no device-owner/MDM, no iOS, no backend, no accounts, no ads, no map SDK.

---

## 2. Technical design (Anchor-specific)

### Mode engine: `:core:engine` (pure Kotlin/JVM, no Android deps)
- A state machine. **Inputs**: trigger events (`ScheduleStarted/Ended`, `PlaceEntered/Exited`, `DrivingStarted/Stopped`, `DeviceConnected/Disconnected`, `SpaceEntered/Exited`), user intents (`HoldToStart`, `AnchorTapped(hash)`, `EmergencyUnlock`, `RecoveryRequested/Elapsed`, `TimerElapsed`) and an injected `Clock`.
- **Outputs**: the new `SessionState` plus a list of **commands** (`ApplyAppPolicy`, `ClearAppPolicy`, `SetDnd`, `SetRinger`, `StartHoldingNotifications`, `ReleaseHeldNotifications`, `ScheduleTimer`, …) that Android-side executors carry out.
- Table-driven tests for every transition, conflict rule, quota and recovery path. Coverage ≥ 90% on this module.

### Platform adapters (thin; each converts platform signals into engine inputs)
- `:core:nfc`: reader mode, NDEF write + AAR, UID hashing (SHA-256 with a per-install salt). Behind an interface with a fake. Add a **debug-only "simulate anchor tap"** (deep link or broadcast usable via `adb`) so flows can be tested without hardware. Write a threat-model ADR: tags can be cloned, which is acceptable for a self-control tool.
- `:core:qr`: QR generation + CameraX/ML Kit scanning.
- `:core:triggers`: schedule (AlarmManager), driving (Activity Recognition), Bluetooth/Spaces (CompanionDeviceManager / BLE scans), places (Geofencing). Each has its own receiver/service that only forwards events.
- `:service:blocker`: the AccessibilityService (foreground app detection, block screen, strict-mode guards). Decide in an ADR between an overlay (`TYPE_ACCESSIBILITY_OVERLAY`) and a full-screen activity for the block screen, considering background-activity-launch rules.
- `:service:notifications`: the NotificationListenerService.
- Installed apps come from `<queries>` with the launcher intent. **Do not use `QUERY_ALL_PACKAGES`.**
- Persistence: Room (modes, anchors, places, spaces, sessions, events, unlock history) + DataStore (settings).

### Module layout
```
app
build-logic/            (convention plugins)
core/common  core/model  core/data  core/database  core/datastore
core/designsystem  core/ui  core/testing
core/engine  core/nfc  core/qr  core/triggers
service/blocker  service/notifications
feature/onboarding  feature/home  feature/modes  feature/anchors
feature/triggers  feature/insights  feature/settings
widget                  (Glance + QS tile + shortcuts)
baselineprofile  benchmark
```

---

## 3. Engineering standards (apply to every PR)

- **Kotlin + Jetpack Compose only.** No XML layouts except where the platform requires XML.
- **Material 3**: dynamic colour, light/dark, edge-to-edge, predictive back. **Adaptive layouts** using window size classes (Android 17 removes the opt-out from resizability on large screens), so check phone, foldable and tablet.
- **Architecture**: UI (Compose + ViewModel) → domain (use cases only where they add value) → data (repositories) → sources. **Unidirectional data flow**: each ViewModel exposes one `StateFlow<UiState>`, user actions are functions, and one-off effects go through a `Channel`. No business logic in composables, services or receivers.
- **Stack**: Hilt, Coroutines/Flow (inject dispatchers), Room (exported schemas + migration tests), DataStore, WorkManager, type-safe Navigation Compose, kotlinx.serialization, Vico for charts.
- **Versions**: Gradle version catalog + convention plugins in `build-logic`. **Look up the latest stable versions at setup time** and don't rely on memory. No alpha/beta libraries unless an ADR justifies them.
- **Verify every platform API against current developer.android.com docs before using it** (especially AutomaticZenRule, CompanionDeviceManager presence, exact alarms, foreground-service types and background-launch rules). Record decisions as ADRs in `docs/adr/NNNN-title.md`.
- **Quality gates**: Spotless (ktlint), Detekt, Android Lint (errors fail the build), unit tests (JUnit4, Turbine, coroutines-test, fakes preferred over mocks), Robolectric + **Roborazzi** screenshot tests for every screen (light/dark, phone/tablet), Compose UI tests for key flows, **Kover** gate (≥ 90% `core:engine`, ≥ 80% other pure-Kotlin modules, ≥ 60% overall).
- One aggregate Gradle task, **`./gradlew ciCheck`**, runs exactly what CI runs.
- **Privacy check in CI**: fail the build if the merged release manifest contains `android.permission.INTERNET`. If a dependency adds it, stop and ask me rather than silently stripping it.
- **Accessibility of our own UI**: content descriptions, 48 dp touch targets, TalkBack order, works at 200% font scale.
- All user-facing strings in resources (English + Hindi).
- Debug builds: StrictMode + LeakCanary.
- **Security**: no secrets in the repo, release signing from env/GitHub secrets, `PendingIntent` immutability flags, no unnecessarily exported components. The NFC intent filter must not let other apps forge an "anchor tap" (validate the tag hash, not just the URI).

---

## 4. First session: preflight, bootstrap, GitHub setup, backlog

### 4.1 Preflight
Check `git`, `gh auth status` (scopes: `repo`, `workflow`; `project` is optional), JDK (whatever the current AGP needs), the Android SDK (`ANDROID_HOME`, `sdkmanager`) and `adb devices`. **If anything is missing, stop and tell me exactly what to install or run. Don't work around it.** NFC can't be emulated, so a physical NFC phone over adb is strongly preferred. If one is connected, use it for verification yourself.

### 4.2 Bootstrap commit (the ONLY direct push to `main`)
- Gradle wrapper, `build-logic` convention plugins, version catalog, every module from §2 as a compiling skeleton, and an app shell that builds and launches.
- Spotless, Detekt, Lint, Kover, Roborazzi wired up, plus the `ciCheck` task.
- Files: `README.md` (pitch, "inspired by Brick" credit line, features, roadmap, build steps, privacy statement, Mermaid module graph), `LICENSE`, `CONTRIBUTING.md` (describes the loop below), `SECURITY.md`, `.editorconfig`, `.gitignore` (keystores, `local.properties`, build outputs), `CODEOWNERS` (`* @<login>`), `docs/PROJECT_BRIEF.md`, `docs/architecture.md`, `docs/adr/0001-record-architecture-decisions.md`.
- `.github/`:
  - `ISSUE_TEMPLATE/feature.yml`, `bug.yml`, `chore.yml`, `config.yml` (YAML issue forms)
  - `pull_request_template.md` (Closes #, What & why, How tested, Screenshots, Device test steps, Risks, checklist)
  - `workflows/ci.yml`: on PRs and pushes to `main`. Job name **`ci`**. Uses `gradle/actions/setup-gradle` caching and concurrency with cancel-in-progress. Runs `./gradlew ciCheck assembleDebug` and the INTERNET-permission check, and uploads the debug APK + reports.
  - `workflows/instrumented.yml`: nightly + manual. Runs connected tests on an emulator (not a required check).
  - `workflows/release.yml`: on `v*` tags. Builds a release APK/AAB. Signs it only if the secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` exist; otherwise attaches the debug APK. Creates a GitHub Release with generated notes.
  - `dependabot.yml`: Gradle + GitHub Actions, weekly, grouped.
- Claude Code setup:
  - `CLAUDE.md`, **kept short**: what the app is, key commands, architecture rules, the loop, definition of done, don'ts. It points to `docs/` for detail.
  - `.claude/agents/android-reviewer.md`, the review subagent (spec in §6).
  - `.claude/settings.json` with a permissions allowlist for `./gradlew`, `git`, `gh` and `adb` commands. Check the current Claude Code settings docs for the exact syntax.

### 4.3 GitHub setup
1. `gh repo create anchor-android --public --source . --push`, with a description and topics (`android`, `kotlin`, `jetpack-compose`, `nfc`, `digital-wellbeing`, `focus`, `material3`).
2. Repo settings via `gh repo edit`: squash merge only, delete branch on merge, auto-merge on, wiki off.
3. **Once `ci` has run on `main` at least once**, protect `main` via `gh api` (ruleset or classic branch protection): require a PR, **0 required approvals** (GitHub doesn't allow approving your own PR, so the review step in §5 takes the place of approval), require the `ci` status check to pass and be up to date, linear history, block force-pushes and deletion.
4. Labels: replace the GitHub defaults with `type:feature`, `type:bug`, `type:chore`, `type:docs`, `type:test`, `type:perf`, `area:engine`, `area:nfc`, `area:triggers`, `area:blocker`, `area:notifications`, `area:ui`, `area:data`, `area:infra`, `priority:p0`/`p1`/`p2`, `size:S`/`M`/`L`, `needs-device-test`, `policy-risk`, `tech-debt`, `blocked`.
5. Milestones from §7, each with a one-paragraph goal.
6. Optional: a GitHub Project board (Backlog / In progress / In review / Done) if `gh` has the `project` scope. Otherwise skip it and tell me how to enable it.
7. **Create the full backlog as issues** from §7. Each issue has context, testable acceptance criteria as checkboxes, technical notes, a test plan, labels, a milestone and dependencies ("Blocked by #x"). Every issue must fit in **one PR of ≤ ~400 changed lines** (excluding generated files). Split anything bigger.

---

## 5. The loop (every change after the bootstrap commit)

1. **Pick.** `git switch main && git pull`. If you have an open PR (`gh pr list --author @me`), finish it first. Otherwise take the next open issue in the current milestone whose blockers are closed. Assign it to yourself and comment a 3–5 line plan on the issue.
2. **Branch.** `gh issue develop <n> --checkout --name <type>/<n>-<slug>` (type: `feat|fix|chore|docs|test|perf|refactor`).
3. **Build.** Write tests first for pure logic (engine transitions, repositories, ViewModels). Make small Conventional Commits, each referencing `#<n>`.
4. **Local gate.** `./gradlew spotlessApply ciCheck` must be green before you push. **Never get green by deleting or skipping tests, lowering thresholds, adding lint baselines or `@Suppress`.** Fix the cause, or explain in the PR and open a `tech-debt` issue.
5. **Raise PR.** `gh pr create`:
   - Title: `<type>(<scope>): <summary> (#<n>)`
   - Body from the template: `Closes #<n>`, what & why, how it was tested, screenshots for UI changes (Roborazzi images, or `adb exec-out screencap -p` from a device), manual device steps if labelled `needs-device-test`, risks.
   - Same labels and milestone as the issue.
6. **Review.** Never skipped, and never done only by the context that wrote the code:
   - a. Run the built-in **`/code-review`** on the PR with `--comment` so findings land as inline PR comments (e.g. `/code-review high --comment <pr>`). Wait for it to finish.
   - b. Delegate to the **`android-reviewer` subagent**. It reviews `gh pr diff <pr>` against the issue's acceptance criteria and its checklist, then posts one review with `gh pr review <pr> --comment --body-file <file>`. Findings are tagged **BLOCKER / MAJOR / NIT**, with a met/not-met line for each acceptance criterion.
   - c. Fix every BLOCKER and MAJOR in new commits. Reply to each finding with the fixing commit SHA or a reasoned rejection. For NITs, fix them if cheap; otherwise open a `tech-debt` issue and link it.
   - d. If anything was a BLOCKER, run (b) again on the new diff. **Max 2 rounds.** If it's still blocked, stop and ask me.
   - For `size:S` docs/chore PRs, step (b) alone is enough.
7. **Merge.** `gh pr checks <pr> --watch`. When green, `gh pr merge <pr> --squash --delete-branch` with a concise squash message. Confirm the issue auto-closed.
8. **Repeat.**

**At the end of each milestone:** tag `v0.<m>.0` (the final milestone is `v1.0.0`) so `release.yml` publishes an APK. Update the README roadmap. Send me a short report: what shipped, screenshots, known issues, and **the exact device tests I should run** (NFC taps, driving, Bluetooth, places). Any bugs I report become issues and go through the same loop.

### Starting or resuming a session
Read `CLAUDE.md` and `docs/PROJECT_BRIEF.md`. Run `gh pr list --state open` and `gh issue list --milestone "<current>" --state open`. Finish open PRs first, then continue the loop. GitHub issues are the source of truth for progress.

---

## 6. `android-reviewer` subagent (create at `.claude/agents/android-reviewer.md`)

Frontmatter: `name: android-reviewer`, a short `description` ("Reviews a PR diff for an Android Kotlin/Compose app against its issue's acceptance criteria and the project checklist"), `tools: Read, Grep, Glob, Bash`, `model: inherit`.

Its system prompt makes it a strict senior Android reviewer. It reads the issue (`gh issue view`), the diff (`gh pr diff`) and the surrounding code, then checks:
- **Acceptance criteria**: each one met or not met, with evidence (file:line or test name).
- **Correctness**: lifecycle, coroutine scopes/cancellation, main-thread work, leaks, process death, config changes, time zones/clock changes, edge cases.
- **Engine invariants**: one active session; automatic triggers never override manual/anchor sessions; state persisted before side effects; emergency/recovery path always reachable; essentials never blocked.
- **Architecture**: layering, UDF, no logic in composables/services/receivers, DI, module boundaries; platform adapters stay thin.
- **Platform & policy**: runtime/special permissions requested only when needed, API-level guards, background limits, foreground-service types, exact-alarm handling, exported components, PendingIntent flags, NFC intent forging, the Play disclosure for accessibility and background location, **no INTERNET**, and no notification content stored unless the user opted in.
- **Tests**: meaningful assertions, transition tables, screenshot tests for UI changes.
- **UI**: accessibility, string resources, dark mode, adaptive layout.
- **Performance/battery**: no polling, no unnecessary wakeups or foreground services, accessibility event cost.

Output: a summary verdict (APPROVE-equivalent / CHANGES NEEDED), the acceptance-criteria table, then the findings list tagged BLOCKER / MAJOR / NIT with file:line references.

---

## 7. Backlog (create these as issues; split further where needed)

**M0 – Foundation** (no tag)
- Design system: M3 theme, dynamic colour, typography, core components (incl. hold-to-start button with haptic progress), screenshot tests
- App shell: type-safe navigation, adaptive scaffold, placeholder screens
- `core:common` + `core:testing` (MainDispatcherRule, fakes, fake Clock, Roborazzi helpers)
- `docs/architecture.md` with Mermaid module graph; ADRs for state management/UDF and the engine command pattern

**M1 – Modes & blocking core** → `v0.1.0`
- Domain model + Room schema + repositories
- Engine v1: manual start/stop, timer, emergency unlock quota, essentials (table-driven tests)
- Installed-apps provider via `<queries>` (no `QUERY_ALL_PACKAGES`)
- Modes list + editor (block/allow list, app picker, templates, priority order)
- Blocker AccessibilityService: all-windows foreground detection, essentials allowlist, block screen (+ ADR overlay vs activity)
- Hold-to-start + active-session screen + ongoing notification
- Emergency unlock flow with friction
- Onboarding: disclosure & consent, enable accessibility, restricted-settings + OEM battery guides

**M2 – Anchors** → `v0.2.0`
- NFC pairing: read UID, write NDEF + AAR when writable, name, list/unpair
- Tap to start/stop: foreground reader mode + background `NDEF_DISCOVERED`, wired to the engine; debug "simulate tap"
- Exit rule "requires anchor" (any / specific)
- Lost-anchor 24 h recovery + re-pair flow
- QR anchor fallback: generate + export printable QR, scan with CameraX + ML Kit
- Threat-model ADR

**M3 – Automatic modes** → `v0.3.0`
- Schedule trigger (alarms, exact-alarm permission handling, boot/update re-registration)
- Driving trigger (Activity Recognition transitions)
- Bluetooth device trigger (CompanionDeviceManager presence / ACL fallback)
- Spaces: BLE proximity with RSSI threshold, hysteresis, dwell (+ ADR on scan approach)
- Places: geofences saved from the current location; background-location flow; `policy-risk` ADR
- Engine conflict resolution + priorities (table-driven tests) + UI to explain why a mode is or isn't active

**M4 – Quiet actions** → `v0.4.0`
- DND via `AutomaticZenRule` (typed rules where supported)
- Ringer control
- Notification holding + "While you were anchored" summary
- Driving auto-reply via RemoteInput (stretch, opt-in, rate-limited)

**M5 – Strict mode, insights & surfaces** → `v0.5.0`
- Strict-mode guards (App info, uninstall, access settings, date & time) with rule-based Settings detection + fixtures; **ask me before merging uninstall protection**
- Weekly Activity insights (time anchored, per-mode sessions, block attempts, emergency unlocks, streaks)
- Glance widget, Quick Settings tile, app shortcuts
- Permission health screen
- Hindi localisation

**M6 – Hardening & release** → `v1.0.0`
- Resilience: process death, reboot, app update, time/time-zone changes, OEM battery killers; instrumented tests for engine ↔ service wiring
- Battery audit (`dumpsys batterystats` / Battery Historian notes) → `docs/performance.md`
- Baseline Profile + Macrobenchmark (startup)
- Play readiness: disclosures, accessibility + background-location declarations in `docs/play/`, data-safety answers, privacy policy on GitHub Pages, store listing copy + screenshots
- README final: demo GIF (NFC tap → apps blocked → tap to release), architecture, key decisions, benchmark numbers
- Release `v1.0.0`

---

## 8. Stop and ask me ONLY when
- Preflight fails, or a tool/auth/scope is missing.
- An action is irreversible or reaches beyond this repo (deleting/renaming the repo, force-push, anything in Play Console).
- Release signing: ask me to create the keystore and add the secrets. Never generate or commit one yourself.
- Device-only verification is needed (NFC, driving, Bluetooth, geofences) and no phone is connected. Batch these at milestone end, with exact steps.
- A Play-policy or privacy trade-off (anything labelled `policy-risk`), and always before merging strict-mode uninstall protection.
- A dependency would add `INTERNET` to the merged manifest.
- The same CI failure happens 3 times, or a review is still blocked after 2 rounds.

Otherwise keep working through the loop without asking.

## 9. Done means
`v1.0.0` is tagged with an installable APK on GitHub Releases. All M0–M6 issues are closed through reviewed PRs (stretch items may stay open, labelled). CI is green on `main`. The README shows the NFC demo GIF, architecture and decisions. There is no `INTERNET` permission. There is always a working recovery path. Everything is documented well enough for a reviewer to understand the project in 5 minutes.

**Start now with §4.1 Preflight.**
