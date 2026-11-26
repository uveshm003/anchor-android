# Task list

Since 2026-11-21, **new work is tracked here instead of in new GitHub issues.** The original backlog stays
in GitHub issues (milestones M0–M6) and is completed through PRs that close them; nothing new is added there.

## How to use

- Add a task under its milestone as `- [ ] **T-<n>** Title`. `<n>` is the next number after the highest
  `T-<n>` on `main` **and in open PRs** (`gh pr list --search "T-"`); numbers are never reused. `ciCheck` fails on
  duplicate ids.
- Give it the **same bar as a backlog issue**: testable acceptance criteria as `- [ ]` sub-bullets, plus a
  `Test plan:` line for code tasks. The PR must meet them.
- Optional `Blocked by: #<n>` / `T-<n>` sub-line; the loop only picks unblocked items.
- **Claim** a task by opening a draft PR early whose body says `Completes T-<n>`; check `gh pr list` first so
  nobody else has claimed it. Edits to this file only reach `main` when a PR merges, so the draft PR is the
  visible claim.
- Mark feature/UX tasks **`needs-approval`**: the plan goes to the user first (see `CLAUDE.md`). Record the
  user's OK in the draft PR's description as soon as it's given (`Plan approved: …`).
- A PR that completes a task ticks it here (moving it to *Done*) and says `Completes T-<n>` in its body.
- Follow-ups, tech debt and bugs reported by the user go here too.

## Open

### M0 – Foundation

*(none yet)*

### M1 – Modes & blocking core

*(none yet)*

### M6 – Hardening & release

- [ ] **T-2** Deleted screenshot goldens come back after a build (tooling).
  - [ ] After deleting a golden and its test, `ciCheck` no longer restores the PNG into `src/test/screenshots`
    (Roborazzi outputs restored from the build cache)
  - Test plan: delete a golden, run `ciCheck` twice with the cache warm, `git status` stays clean
- [ ] **T-3** Lint occasionally crashes in K2 analysis of a test file ("Error while resolving FirRegularClassImpl").
  - [ ] Root cause found (lint 32.5 / Kotlin 2.4, parallel lint workers) and fixed or worked around without
    disabling lint on test sources
  - Test plan: 10 consecutive `ciCheck` runs without the crash
- [ ] **T-4** Compose BOM 2026.11.01 (Dependabot #117) needs `compileSdk` 37.1.
  - [ ] `compileSdk` set to API 37.1 through the convention plugin (AGP 9 minor-API DSL); platform installed in CI
  - [ ] BOM bumped; screenshot goldens re-recorded if rendering changed; `ciCheck` green

## Done

### M0 – Foundation

- [x] **T-1** Decide on Android Studio's local Gradle changes. The user chose to commit both (2026-11-23).
  - [x] `org.gradle.tooling.parallel=true` (`gradle.properties`) and `gradle/gradle-daemon-jvm.properties`
    (pins the Gradle daemon to Java 25) are either committed in a chore PR or discarded
  - [x] If committed: CI still green, and `CONTRIBUTING.md` names the JDK the daemon uses
