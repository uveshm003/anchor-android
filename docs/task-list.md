# Task list

Since 2026-11-21, **new work is tracked here instead of in new GitHub issues.** The original backlog stays
in GitHub issues (milestones M0–M6) and is completed through PRs that close them; nothing new is added there.

## How to use

- Add a task under its milestone as `- [ ] T-<n> Title`, with one line on what "done" means.
- Mark feature/UX tasks **`needs-approval`**: the plan goes to the user first (see `CLAUDE.md`).
- A PR that completes a task ticks it here (moving it to *Done*) and mentions `T-<n>` in its title or body.
- Follow-ups from reviews and tech debt go here too.

## Open

### M0 – Foundation

- [ ] **T-1** Decide on Android Studio's local Gradle changes: `org.gradle.tooling.parallel=true` in
  `gradle.properties` and `gradle/gradle-daemon-jvm.properties` (pins the Gradle daemon to Java 25). Done when
  they are committed as a chore or discarded. *Waiting for the user.*

### M1 – Modes & blocking core

*(none yet)*

## Done

*(none yet)*
