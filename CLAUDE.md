# Anchor: Claude Code guide

Android app (Kotlin + Jetpack Compose, minSdk 31, target 37). Tap the phone on an NFC sticker or QR
"anchor" to start a focus mode that blocks chosen apps; tap again to end it. On-device only.
**Source of truth: `docs/PROJECT_BRIEF.md`.** Architecture: `docs/architecture.md`. Decisions: `docs/adr/`
(UDF: [ADR 0002](docs/adr/0002-state-management-and-udf.md); engine: [ADR 0003](docs/adr/0003-engine-command-pattern.md)).

## Start / resume a session
1. Read this file and `docs/PROJECT_BRIEF.md`.
2. `gh pr list --state open`, `gh issue list --milestone "<current>" --state open` and `docs/task-list.md`.
3. Finish open PRs first, then continue the loop. Work items are the existing GitHub issues plus the task list.

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
- `core:engine` is pure Kotlin: `reduce(EngineState, Input, Environment) -> Transition(state, commands,
  outcome)`. Adapters only `submit` inputs and execute commands through the `core:engine` ports; they never
  depend on `core:data` (ADR 0003).
  No business logic in composables, services or receivers.
- Persist session state **before** running side effects. One active session. Automatic triggers
  never override a manual/anchor session. Essentials (dialer, emergency, launcher, system UI, Anchor)
  are never blocked. Emergency unlock and recovery are always reachable.
- Inject dispatchers, `Clock` and platform managers; prefer fakes over mocks.
- Features never depend on each other. Build config lives in `build-logic/` convention plugins.
- Verify platform APIs on developer.android.com before use; record decisions as ADRs.

## The loop
**Work items:** the existing GitHub issues (backlog M0–M6), issues filed by outside reporters, and
`docs/task-list.md`. **Never create new GitHub issues**: new work, review follow-ups, tech debt and bugs the user
reports go into `docs/task-list.md`, with testable acceptance criteria like an issue.
(This overrides the brief's issue/squash parts, including §4.2's feature/chore issue forms, §4.3 step 2 (squash
only) and step 7 for new work, §5 steps 1–2 and 4–7 (tech-debt issues, `Closes #<n>`/issue labels for new work,
issue-only acceptance criteria, squash), "bugs I report become issues", "GitHub issues are the source of truth"
and §6's issue-only inputs. The brief file stays unchanged; process overrides live here, ADRs cover architecture
and platform decisions.)

**Features and UX need the user's OK first:** for any user-facing feature, screen, behaviour change or
product/UX decision (including a fix, perf change or ADR that changes what the user sees or can do), post the
plan in chat, ask the user and wait for approval before writing code. Record the OK right away where it's
visible: a comment on the backlog issue, or the `Plan approved:` line of the task's draft PR. While waiting, take the next item that doesn't need approval; if there is none,
stop and report. Tests, build tooling, refactors and docs proceed without asking.
(This overrides brief §8's "keep working".)

Pick the next unblocked item in the current milestone and claim it (assign / comment on a backlog issue, or a
draft PR saying `Completes T-<n>` for a task-list item; check `gh pr list` first) (→ wait for OK if feature/UX) → branch `<type>/<n>-<slug>` (issue number or
`t<n>` for a task-list item) → tests first → **about 4 meaningful Conventional Commits per PR** (e.g. code /
tests / screenshots or resources / docs + task list; bundle small related items; never filler commits; every
commit builds, check with `git rebase -x "./gradlew assembleDebug -q" origin/main`; each subject references
`#<n>` or `T-<n>`) → `./gradlew spotlessApply ciCheck` → `gh pr create` (title `<type>(<scope>): <summary> (#<n>)`
or `(T-<n>)`, template, labels + milestone) → review: `/code-review high --comment <pr>`, then the
`android-reviewer` subagent (`size:S` docs/chore PRs: subagent only) → fix BLOCKER/MAJOR, reply to each finding
with the commit SHA and subject (SHAs change when rebased) → re-review if a BLOCKER (max 2 rounds) →
`gh pr update-branch <pr> --rebase` if `main` moved (then `git pull --rebase` locally before the next commit) →
`gh pr checks <pr> --watch` → `gh pr merge <pr> --rebase --delete-branch` (rebase merge keeps each commit on
`main`). Review-fix commits land as extra commits on top of the ~4; never rewrite history to fold them in
(`update-branch --rebase` is the one allowed rewrite). If a rebase merge is refused, stop and ask; never squash
instead. Size labels: `size:S` < 100 changed lines, `size:M` < 250, `size:L` ≤ ~400 (generated files excluded).
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
- No new GitHub issues (use `docs/task-list.md`); no squash merges (rebase merge keeps the commits).
- No `Co-Authored-By` trailers or "Generated with Claude Code" lines in commits or PRs.
- Ask the user before: implementing any feature/UX work (plan first), merging strict-mode uninstall protection, any `policy-risk` trade-off, a
  dependency that adds INTERNET, release signing, or after 3 identical CI failures.
