# 6. Block screen: a full-screen activity started by the accessibility service

Date: 2026-11-23 · Status: Accepted · Issues: #21, #22, #23

## Context

When a blocked app becomes visible, Anchor must cover it quickly and explain the session. The two options are an
accessibility overlay window (`TYPE_ACCESSIBILITY_OVERLAY`, added by the service through `WindowManager`) or a
full-screen activity started by the service.

Docs checked 2026-11-23:
- Background activity launch restrictions list an exemption when "the app has a service that is bound by the
  system", which includes `AccessibilityService`
  (https://developer.android.com/guide/components/activities/background-starts).
- `AccessibilityService.getWindows()` returns all interactive windows (split-screen, PiP, bubbles, desktop
  windows) when the service sets `flagRetrieveInteractiveWindows` and `canRetrieveWindowContent`
  (https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#getWindows()).

## Decision

Use a **full-screen activity** (`BlockActivity`, own task affinity, excluded from Recents, single task),
started by `BlockerService` whenever a blocked package is visible in any application window.

| | Activity (chosen) | Accessibility overlay |
|---|---|---|
| Background start | Allowed (system-bound service exemption) | No activity needed |
| Back / Home / Recents | Normal: Back and "Go home" open the launcher; returning to the app shows it again | Overlay stays until removed; easy to trap the user |
| TalkBack, input, IME | Standard activity semantics | Needs manual focus and input handling |
| Multi-window | Covers the screen; essentials stay reachable via Home | Covers everything, including an essential in the other half |
| Compose | `setContent` as usual | Needs a hand-made lifecycle/saved-state owner |

## Consequences

- If the user goes back to the blocked app, the service starts the block screen again; each return counts as
  one block attempt (debounced per foreground change).
- In split-screen the block screen takes over the screen rather than one half. Acceptable for the MVP; revisit
  with per-window overlays if users ask.
- `canRetrieveWindowContent` is only used to read each window's package name; no on-screen text is read.
  The Play disclosure (#27) says so.
