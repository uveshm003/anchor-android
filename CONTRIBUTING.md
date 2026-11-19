# Contributing to Anchor

Thanks for helping. Anchor is built issue by issue, and every change, including the maintainer's,
goes through the same loop.

## The loop

1. **Pick an issue.** Open, in the current milestone, with all "Blocked by #x" issues closed. Comment
   that you're taking it, with a 3–5 line plan.
2. **Branch** from `main`: `<type>/<issue>-<slug>`, where type is
   `feat|fix|chore|docs|test|perf|refactor` (for example `feat/12-hold-to-start`).
   `gh issue develop <n> --checkout --name <branch>` links the branch to the issue.
3. **Build it.** Write tests first for pure logic (engine transitions, repositories, ViewModels).
   Make small [Conventional Commits](https://www.conventionalcommits.org/) that reference the issue:
   `feat(engine): end session on anchor tap (#12)`.
4. **Local gate.** `./gradlew spotlessApply ciCheck` must pass before you push.
5. **Open a PR** titled `<type>(<scope>): <summary> (#<n>)`, fill in the template (it starts with
   `Closes #<n>`), and copy the issue's labels and milestone.
6. **Review.** Every PR is reviewed against the issue's acceptance criteria and the checklist in
   [`.claude/agents/android-reviewer.md`](.claude/agents/android-reviewer.md). Findings are tagged
   BLOCKER / MAJOR / NIT. BLOCKERs and MAJORs are fixed in new commits; each finding gets a reply with
   the fixing commit or a reasoned rejection. NITs are fixed or turned into `tech-debt` issues.
7. **Merge** with squash once `ci` is green. The issue closes automatically.

Each issue should fit in one PR of roughly 400 changed lines or fewer (generated files excluded).
If it grows, split it.

## Never get green by…

- deleting, skipping or `@Ignore`-ing tests,
- lowering coverage thresholds,
- adding lint or detekt baselines or `@Suppress`.

Fix the cause. If you truly can't, explain why in the PR and open a `tech-debt` issue.

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
- **No `INTERNET` permission.** If a dependency adds it, CI fails. Don't strip it; raise it on the
  issue.
- Verify platform APIs against current developer.android.com docs and record significant decisions
  as ADRs in `docs/adr/` ([ADR 0001](docs/adr/0001-record-architecture-decisions.md)).
- Ask for a permission only when the user turns on the feature that needs it.

## Reporting bugs

Use the **Bug report** issue form. Include the device, Android version and Anchor version. For
security problems use private reporting instead (see [SECURITY.md](SECURITY.md)).
