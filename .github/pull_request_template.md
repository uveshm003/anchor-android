<!-- Keep one: -->
Closes #<issue>
Completes T-<n>

<!-- Feature/UX work only (otherwise delete): quote the user's approval of the plan. -->
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
- [ ] 3–4 meaningful commits, each building and referencing `#<n>`/`T-<n>`; `docs/task-list.md` updated if needed
- [ ] UI: strings in English + Hindi, screenshots updated, accessible (labels, 48 dp, 200 % font)
- [ ] No `INTERNET`; permissions only when the feature is turned on
