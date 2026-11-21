Closes #<!-- backlog issue number --> <!-- or: Completes T-<n> from docs/task-list.md -->

<!-- Feature/UX work: quote the user's approval of the plan here. -->
Plan approved: 

## What & why

<!-- What changed and why. Link ADRs if a decision was made. -->

## How tested

<!-- Unit / Robolectric / screenshot / instrumented tests added or changed, and what you ran. -->

## Screenshots

<!-- UI changes: Roborazzi images or `adb exec-out screencap -p`. Delete if not applicable. -->

## Device test steps

<!-- Required if labelled `needs-device-test`: exact steps someone can follow on a phone. -->

## Risks

<!-- Behaviour, policy, battery or migration risks, and how they are mitigated. -->

## Checklist

- [ ] `./gradlew spotlessApply ciCheck` passes locally
- [ ] About 4 meaningful commits (fewer is fine for a small change; no filler; each builds and references `#<n>`/`T-<n>`); `docs/task-list.md` updated if this completes or adds tasks
- [ ] Tests cover the acceptance criteria (no tests skipped, no thresholds lowered, no baselines or `@Suppress`)
- [ ] User-facing strings are in resources, in English and Hindi
- [ ] Screenshot tests added/updated for UI changes (light/dark, phone/tablet)
- [ ] Accessibility: content descriptions, 48 dp targets, TalkBack order, 200 % font scale
- [ ] No `INTERNET` permission; no new exported components or mutable `PendingIntent`s without reason
- [ ] Permissions are requested only when the feature that needs them is turned on
- [ ] Docs/ADR updated if a decision was made
