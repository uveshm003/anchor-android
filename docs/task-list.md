# Task list

Since 2026-11-21, **new work is tracked here instead of in new GitHub issues.** The original backlog stays
in GitHub issues (milestones M0–M6) and is completed through PRs that close them; nothing new is added there.

## How to use

- Add a task under its milestone as `- [ ] **T-<n>** Title`. `<n>` is the next number after the highest
  `T-<n>` on `main` **and in open PRs** (`gh pr list --search "T-"`); numbers are never reused. `ciCheck` fails on
  duplicate ids.
- Give it the **same bar as a backlog issue**: testable acceptance criteria as `- [ ]` sub-bullets, plus a
  `Test plan:` line for code tasks. Reviews check the PR against these.
- Optional `Blocked by: #<n>` / `T-<n>` sub-line; the loop only picks unblocked items.
- **Claim** a task by opening a draft PR early whose body says `Completes T-<n>`; check `gh pr list` first so
  nobody else has claimed it. Edits to this file only reach `main` when a PR merges, so the draft PR is the
  visible claim.
- Mark feature/UX tasks **`needs-approval`**: the plan goes to the user first (see `CLAUDE.md`). Record the
  user's OK in the draft PR's description as soon as it's given (`Plan approved: …`).
- A PR that completes a task ticks it here (moving it to *Done*) and says `Completes T-<n>` in its body.
- Follow-ups from reviews, tech debt and bugs reported by the user go here too.

## Open

### M0 – Foundation

- [ ] **T-1** Decide on Android Studio's local Gradle changes. *Waiting for the user.*
  - [ ] `org.gradle.tooling.parallel=true` (`gradle.properties`) and `gradle/gradle-daemon-jvm.properties`
    (pins the Gradle daemon to Java 25) are either committed in a chore PR or discarded
  - [ ] If committed: CI still green, and `CONTRIBUTING.md` names the JDK the daemon uses

### M1 – Modes & blocking core

*(none yet)*

## Done

*(none yet)*
