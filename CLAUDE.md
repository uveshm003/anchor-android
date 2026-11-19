# Anchor: Claude Code guide

Android app (Kotlin + Jetpack Compose, minSdk 31, target 37). Tap the phone on an NFC sticker or QR
"anchor" to start a focus mode that blocks chosen apps; tap again to end it. On-device only.
**Source of truth: `docs/PROJECT_BRIEF.md`.** Architecture: `docs/architecture.md`. Decisions: `docs/adr/`
(UDF: [ADR 0002](docs/adr/0002-state-management-and-udf.md); engine: [ADR 0003](docs/adr/0003-engine-command-pattern.md)).

## Start / resume a session
1. Read this file and `docs/PROJECT_BRIEF.md`.
2. `gh pr list --state open` and `gh issue list --milestone "<current>" --state open`.
3. Finish open PRs first, then continue the loop. GitHub issues track progress.

## Commands
- `./gradlew spotlessApply ciCheck`: local gate, identical to CI (format, detekt, lint, tests, screenshots, Kover, no-INTERNET check)
- `./gradlew assembleDebug` / `installDebug`
- `./gradlew recordRoborazziDebug`: re-record screenshot goldens after an intended UI change
- `./gradlew :core:engine:test`, `./gradlew :<module>:testDebugUnitTest`
- `python3 scripts/module-graph.py`: regenerate the module graph after changing module dependencies (`ciCheck`
  fails if it is stale)
- `./gradlew connectedDebugAndroidTest`: on device. If the phone is asleep:
  `adb shell input keyevent KEYCODE_WAKEUP && adb shell wm dismiss-keyguard`

## Architecture rules
- UI (Compose + ViewModel) → domain/engine → data (repositories) → sources. UDF: one
  `StateFlow<UiState>` per ViewModel, actions are functions, one-off effects via `Channel`.
- `core:engine` is pure Kotlin: `(SessionState, Input) -> (SessionState, List<Command>)`. Android
  adapters (services, receivers, NFC) only translate signals into inputs and execute commands.
  No business logic in composables, services or receivers.
- Persist session state **before** running side effects. One active session. Automatic triggers
  never override a manual/anchor session. Essentials (dialer, emergency, launcher, system UI, Anchor)
  are never blocked. Emergency unlock and recovery are always reachable.
- Inject dispatchers, `Clock` and platform managers; prefer fakes over mocks.
- Features never depend on each other. Build config lives in `build-logic/` convention plugins.
- Verify platform APIs on developer.android.com before use; record decisions as ADRs.

## The loop (brief §5)
Pick issue → assign + comment plan → `gh issue develop <n> --checkout --name <type>/<n>-<slug>` →
tests first, small Conventional Commits referencing `#<n>` → `./gradlew spotlessApply ciCheck` →
`gh pr create` (title `<type>(<scope>): <summary> (#<n>)`, template, same labels + milestone) →
review: `/code-review high --comment <pr>`, then the `android-reviewer` subagent (size:S docs/chore:
subagent only) → fix BLOCKER/MAJOR, reply to each finding with the SHA → re-review if a BLOCKER
(max 2 rounds) → `gh pr checks <pr> --watch` → `gh pr merge <pr> --squash --delete-branch`.
End of milestone: tag `v0.<m>.0`, update the README roadmap, report to the user with device tests.

## Definition of done
Acceptance criteria met with tests; `ciCheck` green; screenshots for UI (light/dark, phone/tablet);
strings in English + Hindi; accessible (content descriptions, 48 dp, 200 % font); reviewed; merged.

## Don'ts
- No `INTERNET` permission, analytics, crash SDKs, accounts or backend. Never strip a dependency's
  INTERNET with `tools:node="remove"`; stop and ask.
- No `QUERY_ALL_PACKAGES`, no polling, no always-on foreground service without an ADR.
- Never get green by deleting/skipping tests, lowering thresholds, lint/detekt baselines or `@Suppress`.
- No direct pushes to `main`, no force-pushes, never create or commit a keystore.
- No `Co-Authored-By` trailers or "Generated with Claude Code" lines in commits or PRs.
- Ask the user before: merging strict-mode uninstall protection, any `policy-risk` trade-off, a
  dependency that adds INTERNET, release signing, or after 3 identical CI failures.
