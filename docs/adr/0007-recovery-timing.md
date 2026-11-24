# 7. Lost-anchor recovery: monotonic countdown with a wall-clock limit

Date: 2026-11-24 · Status: Accepted · Issues: #38, #39 · Builds on ADR 0003 §5

## Context

Lost-anchor recovery ends a session 24 hours after the user asks, whatever the exit rule or the emergency quota,
so nobody stays locked out. The wall clock can be moved by the user, so it alone can't measure the delay; the
monotonic clock (`SystemClock.elapsedRealtime()`, "milliseconds since boot, including time spent in sleep")
resets on reboot. `Settings.Global.BOOT_COUNT` tells boots apart. Alarms are cleared on reboot.
Docs checked 2026-11-24: https://developer.android.com/reference/android/os/SystemClock ·
https://developer.android.com/reference/android/provider/Settings.Global#BOOT_COUNT ·
https://developer.android.com/reference/android/app/AlarmManager#ELAPSED_REALTIME_WAKEUP

## Decision

- `EngineState.recovery` stores the request time (wall clock), the monotonic time counted so far, and a stamp
  (boot count + `elapsedRealtime`). Every recovery input and every reconcile re-stamps it: time since the stamp is
  added only if the boot count is the same. Time between the last stamp and a reboot is lost, so **a reboot pauses
  the countdown but never resets it**.
- It completes after **24 h of counted monotonic time**, or when **48 h of wall clock** have passed since the
  request, whichever is first. Moving the clock forward can only shorten it down to 48 h after the request.
- The alarm is `ELAPSED_REALTIME_WAKEUP` (exact when allowed). When it fires, the engine re-checks and schedules
  the next check if needed; the boot reconcile (a `BOOT_COMPLETED` receiver) schedules it again after a reboot.
- Any other session end (anchor, timer, emergency) cancels a pending recovery.

## Consequences

- Recovery always completes, and can't be sped up by changing the clock or rebooting.
- A user who reboots often waits longer than 24 h, up to 48 h. Accepted: the bound is honest and documented.
