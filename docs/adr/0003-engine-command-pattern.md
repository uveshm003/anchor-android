# 3. Mode engine: pure reducer and command pattern

- **Status:** Accepted
- **Date:** 2026-11-19

## Context

The rules at Anchor's core interact in subtle ways (brief §1, §2): one active session, automatic triggers
that must never override a manual or anchor session, priorities, exit rules, the emergency-unlock quota,
lost-anchor recovery, and essentials that are never blocked. The same rules must hold across process death,
reboots and clock changes. Spread across services, receivers and ViewModels they would be untestable;
brief §2 asks for a pure Kotlin engine with ≥ 90 % coverage.

## Decision

**Pure reducer** in `core:engine` (no Android, no coroutines):

```kotlin
fun reduce(state: SessionState, input: Input, now: Instant): Transition
data class Transition(val state: SessionState, val commands: List<Command>, val outcome: Outcome)
```

- `Input` covers trigger events (`ScheduleStarted/Ended`, `PlaceEntered/Exited`, `DrivingStarted/Stopped`,
  `DeviceConnected/Disconnected`, `SpaceEntered/Exited`) and user intents (`HoldToStart`,
  `AnchorTapped(hash)`, `EmergencyUnlock`, `RecoveryRequested/Elapsed`, `TimerElapsed`).
- `Command` describes side effects only: `ApplyAppPolicy`, `ClearAppPolicy`, `SetDnd`, `SetRinger`,
  `StartHoldingNotifications`, `ReleaseHeldNotifications`, `ScheduleTimer`, `CancelTimer`, `RecordUnlock`, …
- `Outcome` says what happened: `Started`, `Ended`, `Ignored(reason)` or `Rejected(reason)`. Rejections
  are values, not exceptions, so the UI can explain them ("Tap your Desk anchor").
- The engine reads time only from the `now` it is given (from the injected `Clock`).

**Session controller** (`core:data`), the only caller of `reduce`:

1. Processes inputs one at a time (a `Mutex`), so concurrent triggers can't interleave.
2. Reduces, then **persists the new `SessionState` before running any command**.
3. Executes commands through `CommandExecutor`s in `core:data`, the services and the trigger adapters.
4. On app start, boot, package update or time change, it restores the persisted state and replays the
   commands that re-establish it (`ApplyAppPolicy`, `ScheduleTimer`, …).

**Executors are idempotent.** A restored session may replay `ApplyAppPolicy` or `ScheduleTimer`, so
running a command twice must equal running it once. A failing executor is logged and reported back as an
input (e.g. `CommandFailed`) rather than rolling back the persisted state. The state reflects what the user
asked for, and adapters converge on it.

**Time.** Inputs carry wall-clock instants from `java.time.Clock` (`SystemZoneClock`). Timers are
scheduled for an instant and re-armed on `TIME_SET`/`TIMEZONE_CHANGED`. Moving the clock forward can
therefore end a timer early; that is accepted for timers (brief: honest friction, not a jail). For the
tamper-sensitive durations (the 24 h lost-anchor delay, the rolling emergency-unlock window), the Android
side additionally records a monotonic reference (`elapsedRealtime` plus a boot counter) and the engine
receives the elapsed duration as part of the input. The exact scheme is decided in #38 (recovery engine).

## Consequences

- Every rule is a table-driven unit test on `reduce`, with no Robolectric or emulator needed.
- Process death between "persist" and "execute" is safe: restore replays idempotent commands.
- Adapters stay thin: a receiver builds an `Input` and calls the controller; a service executes a `Command`.
- Adding a feature usually means a new `Input` or `Command` plus an executor. The reducer's `when` must
  stay exhaustive, so the compiler points at every place that needs a decision.

## Alternatives considered

- **State machines inside Android services:** untestable without devices, and they lose state on process
  death.
- **Event sourcing (persist inputs, rebuild state):** more resilient history, but replay would re-run
  time-dependent inputs. Persisting the resulting state is simpler and enough for our rules; history for
  insights is recorded separately as session events.
