# MVP plan

**User decision (2026-11-23):** "Can you just create working MVP first & change the plan accordingly & then we can
add more & more features??" plus "proceed complete as much as you can in this session I need this app completed
ASAP". This is the user's OK for the MVP plan below, recorded here as `CLAUDE.md` asks; each MVP PR quotes it
on its `Plan approved:` line.

## Goal

The smallest app that does what Anchor promises, end to end:

1. Create a **mode** with a name and the **apps to block**.
2. **Hold to start** it on Home.
3. Opening a blocked app shows the **block screen** (Accessibility service); essentials are never blocked.
4. **Pair an NFC sticker** and **tap it to end** the session (or to start the default mode).
5. **Emergency unlock** (3 per rolling 7 days) so nobody is locked out.
6. **Onboarding** with the Accessibility disclosure and consent.

Tagged `v0.1.0` when done. Later milestones (M3–M6 and the deferred items below) then add features one at a time.

## Order

The MVP reuses the existing backlog issues, in this order (milestone **MVP**). A PR may complete two small,
related issues together.

| # | Issue | MVP scope |
|---|---|---|
| 1 | #3 + #4 App shell | Navigation Compose 2.x type-safe routes (short ADR), `NavigationSuiteScaffold`, placeholder screens |
| 2 | #9 Domain model | Mode, AppPolicy, Session, Anchor, essentials |
| 3 | #10 Room v1 | Modes, sessions, unlock history **and anchors** (nothing has shipped, so no v1→v2 migration for #31) |
| 4 | #11 DataStore | Onboarding state, emergency quota, per-install salt |
| 5 | #12 Repositories | Modes, sessions, unlocks, anchors + fakes |
| 6 | #13 + #14 Engine v1 | Hold-to-start, stop, timer, emergency quota, essentials |
| 7 | #34 Engine: anchor taps | "Requires anchor" exit rule (any anchor) |
| 8 | #15 Session controller | Persist before side effects, restore after process death |
| 9 | #16 Installed apps | `<queries>` launcher apps |
| 10 | #18 + #19 Mode editor + app picker | Name, block list, search; #17 list without reorder |
| 11 | #21 + #22 + #23 Blocker | ADR, all-windows detection, block screen |
| 12 | #24 Home | Hold-to-start and the active-session screen |
| 13 | #27 Onboarding | Prominent disclosure, consent, enable Accessibility |
| 14 | #30 + #32 NFC | Reader mode, salted UID hash, NDEF URI + AAR write |
| 15 | #33 Anchors screen | Pair, name, unpair |
| 16 | #35 Tap to start/stop | Foreground reader mode + background `NDEF_DISCOVERED` |
| 17 | #37 Simulate tap | Debug-only `adb` tap, so flows work on an emulator |
| 18 | #26 Emergency unlock | 10 s hold + typed sentence |

## Deferred until after the MVP

#7 icon and splash, #17 reorder, #20 templates, #25 ongoing notification, #28 battery/OEM guides, #29 threat-model
ADR, #31 (folded into #10), #36 specific-anchor exit rule, #38/#39 lost-anchor recovery, #40/#41 QR anchors,
#84 shadows, #85 fixed colour roles, and all of M3–M6. Until #38/#39 land, the MVP's recovery path is emergency
unlock, and turning off Anchor's Accessibility service in system Settings (there is no strict mode yet).

## Bar

Unchanged: `ciCheck` green (tests, Kover, lint, screenshots), strings in English + Hindi, accessible UI.
