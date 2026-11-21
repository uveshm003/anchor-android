# Contributing to Anchor

Thanks for helping. Every change, including the maintainer's, goes through the same loop.

## Where work comes from

- The original backlog: [GitHub issues](https://github.com/uveshm003/anchor-android/issues) in milestones
  M0–M6. These are completed and closed by PRs, but the maintainer and agents **create no new issues**.
- Outside contributors can still report bugs with the **Bug report** issue form; those are picked up too.
- Everything new (features, follow-ups, tech debt): [`docs/task-list.md`](docs/task-list.md).

## The loop

1. **Pick** the next item in the current milestone whose dependencies are done (a backlog issue, a
   reported bug or a task-list item). For
   anything user-facing (feature, screen, behaviour or UX change), share a 3–5 line plan and get the
   maintainer's OK before writing code.
2. **Branch** from `main`: `<type>/<n>-<slug>`, where type is `feat|fix|chore|docs|test|perf|refactor`
   and `<n>` is the issue number or `t<n>` for a task-list item (for example `feat/24-hold-to-start`).
3. **Build it** in about **4 meaningful commits** per PR, e.g. code, tests, screenshots or resources,
   docs and the task list. Write tests first for pure logic (engine, repositories, ViewModels). Use
   [Conventional Commits](https://www.conventionalcommits.org/) that reference the item:
   `feat(engine): end session on anchor tap (#12)`. Every commit must build; no filler commits.
4. **Local gate.** `./gradlew spotlessApply ciCheck` must pass before you push.
5. **Open a PR** titled `<type>(<scope>): <summary> (#<n>)` (or `(T-<n>)`), fill in the template, and
   use the issue's labels and milestone (for a task-list item: type/area/size labels and the milestone of
   its section).
6. **Review.** Every PR is reviewed against the item's acceptance criteria and the checklist in
   [`.claude/agents/android-reviewer.md`](.claude/agents/android-reviewer.md). Findings are tagged
   BLOCKER / MAJOR / NIT. BLOCKERs and MAJORs are fixed in new commits; each finding gets a reply with
   the fixing commit or a reasoned rejection. NITs are fixed or added to `docs/task-list.md`.
7. **Merge** with **rebase merge** once `ci` is green, so each commit lands on `main` (update the branch
   with a rebase first if `main` moved; never squash). A backlog issue closes automatically; tick the
   task-list item in the same PR.

Each PR should stay around 400 changed lines or fewer (generated files excluded). If it grows, split it.

## Never get green by…

- deleting, skipping or `@Ignore`-ing tests,
- lowering coverage thresholds,
- adding lint or detekt baselines or `@Suppress`.

Fix the cause. If you truly can't, explain why in the PR and add it to `docs/task-list.md`.

## Standards

- Kotlin + Jetpack Compose. No XML layouts except where the platform requires XML.
- Each ViewModel exposes one `StateFlow<UiState>`; user actions are functions; one-off effects go
  through a `Channel`. No business logic in composables, services or receivers.
- All user-facing strings live in resources, in **English and Hindi**.
- New screens get Roborazzi screenshot tests (light/dark, phone/tablet) written with `ScreenshotRule` +
  `ScreenshotVariant.parameters()` from `core:screenshot-testing` (see its KDoc for the pattern). Record with
  `./gradlew recordRoborazziDebug` and commit the PNGs.
- Our own UI must be accessible: content descriptions, 48 dp touch targets, sensible TalkBack
  order, usable at 200 % font size.
- **No `INTERNET` permission.** If a dependency adds it, CI fails. Don't strip it; raise it in the PR.
- Verify platform APIs against current developer.android.com docs and record significant decisions
  as ADRs in `docs/adr/` ([ADR 0001](docs/adr/0001-record-architecture-decisions.md)).
- Ask for a permission only when the user turns on the feature that needs it.

## Reporting bugs

Use the **Bug report** issue form. Include the device, Android version and Anchor version. For
security problems use private reporting instead (see [SECURITY.md](SECURITY.md)).
