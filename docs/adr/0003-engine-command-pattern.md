# 3. Mode engine: pure reducer, command outbox and a single input queue

- **Status:** Accepted
- **Date:** 2026-11-20

## Context

The rules at Anchor's core interact in subtle ways (brief §1, §2): one active session, automatic triggers
that never override a manual or anchor session, priorities, exit rules, the emergency-unlock quota,
lost-anchor recovery, and essentials that are never blocked. They must hold across process death, reboots
and clock changes (brief feature 14). Spread across services, receivers and ViewModels they would be
untestable; brief §2 asks for a pure Kotlin engine with ≥ 90 % coverage.

## Decision

### 1. Pure reducer (`core:engine`)

```kotlin
fun reduce(state: EngineState, input: Input, env: Environment): Transition
data class Environment(val now: Instant, val elapsedRealtime: Duration, val bootCount: Int, val quiet: QuietSnapshot)
data class Transition(val state: EngineState, val commands: List<Command>, val outcome: Outcome)
```

`Environment` is sampled **once per input, when the consumer dequeues it** (§3): wall-clock `now` from the
injected `Clock`, the monotonic reading and boot count (§5), and the device's current ringer/DND state.

- No Android, no coroutines, no I/O. `core:engine` depends only on `core:model` (its current
  `core:common` edge is dropped in #13).
- `EngineState` is everything the rules need and is persisted as a whole: the session (or `Idle`), what
  started it (`startedBy`) and its exit rule, the currently active automatic triggers (so a schedule still
  running when a manual session ends can start, and a trigger ending only ends the session it started),
  the **emergency-unlock history** the quota is computed from, pending recovery, and the values to restore
  at session end (below).
- `Input` covers trigger events (`ScheduleStarted/Ended`, `PlaceEntered/Exited`, `DrivingStarted/Stopped`,
  `DeviceConnected/Disconnected`, `SpaceEntered/Exited`), user intents (`HoldToStart(modeId)`,
  `StopRequested`, `AnchorTapped(hash)`, `EmergencyUnlock`, `RecoveryRequested`, `RecoveryCancelled`),
  answers to earlier commands, which carry the session id they belong to (`TimerElapsed(sessionId)`,
  `RecoveryElapsed(sessionId)`, `CommandFailed(commandId, sessionId)`), and `Reconcile(cause,
  configuredTriggers)` (§4). Inputs carry only platform-supplied facts; time and device state come from
  `Environment`.
- `Command` describes side effects only (`ApplyAppPolicy`, `ClearAppPolicy`, `SetDnd`, `SetRinger`,
  `StartHoldingNotifications`, `ReleaseHeldNotifications`, `ScheduleTimer`, `CancelTimer`, …). Every
  command has a stable `id`.
- `Outcome` tells the UI what happened. It is a sealed type that grows with the rules (e.g. `Started`,
  `Ended`, `Preempted`, `RecoveryScheduled`, `Ignored(reason)`, `Rejected(reason)`, `Degraded`).
  Rejections are values, not exceptions ("Tap your Desk anchor").
- **Essentials:** the reducer never emits an `ApplyAppPolicy` whose blocked set contains an essential
  package (resolved on the Android side and passed in). The blocker independently always allows the
  runtime-resolved dialer, launcher, SMS app, system UI and Anchor (defence in depth).
- **Quota is state, not a side effect:** `EmergencyUnlock` appends to the unlock history inside
  `EngineState`, so the record is persisted in the same transaction as the session end. Insights read
  history from session events.
- **Restore-to values:** when a session starts and **no session is active and no restore is pending**, the
  reducer stores `env.quiet` (the user's ringer/DND) as the restore-to value. If a session starts while
  another is active (preemption) or while a restore is still pending, the existing restore-to value is
  carried over, so Anchor's own SILENT/DND is never recorded as the user's setting. At session end the
  reducer emits `SetRinger`/`SetDnd` with the stored value and marks the restore *pending* until the
  command succeeds. Executors never capture "previous" themselves.

### 2. Ports live in `core:engine`; the controller lives in `core:data`

Adapters (`core:nfc`, `core:qr`, `core:triggers`, `service:*`) must not depend on `core:data`. The
contracts therefore sit next to the types they use:

```kotlin
// core:engine
interface SessionInputs { suspend fun submit(input: Input) }
interface CommandExecutor {
    val handles: Set<KClass<out Command>>
    suspend fun execute(command: Command): ExecutionResult // Done | Failed(reason)
}
```

Exactly one executor handles each command type. The controller builds a registry from the `@IntoSet`
executors at startup and fails fast on a missing or duplicate handler (covered by a test), so "not mine"
and "failed" can never be confused.

`core:data` implements `SessionInputs` (the controller). Each adapter provides its executors with Hilt
`@IntoSet` and calls `SessionInputs.submit` from receivers/services. `app` wires the graph.

### 3. One consumer, persisted outbox

- `submit` only **enqueues** into a `Channel<Input>(UNLIMITED)`. A single coroutine in `@ApplicationScope`
  drains it. For each input it samples `now` once from the injected `Clock`, calls `reduce`, and in **one
  Room transaction** persists the new `EngineState` *and* the transition's commands as pending outbox rows.
  Then it executes the pending commands in order, marking each done after it succeeds.
- Executors are **idempotent and dedupe by command id**. On a crash between persist and execute, the
  pending rows run again on the next start. Replaying a done command is harmless.
- A `Failed` result is fed back **immediately**: the consumer marks the outbox row failed (not pending, so
  it is never blindly replayed) and reduces `CommandFailed(commandId, sessionId)` before it dequeues the
  next input. There is no re-entry into `submit`, so no deadlock and no reordering. The reducer ignores
  failures from a session that has already ended. It never answers `CommandFailed` by re-emitting the
  same command; it records a degraded state, and the next `Reconcile` retries by recomputing the desired
  state.
- Commands from transition N always finish (or fail) before transition N+1 is reduced, so a stale
  `ApplyAppPolicy` can never land after `ClearAppPolicy`.

### 4. Reconcile is an input, so restore is a rule

On app start, `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `ACTION_TIME_CHANGED` and
`ACTION_TIMEZONE_CHANGED`, the controller loads the configured triggers and submits
`Reconcile(cause, configuredTriggers)`. The reducer returns the **complete desired-state command set for any
state**:

- It first ends sessions whose timer already passed (`now ≥ endsAt`), so an expired session is never
  re-applied after a reboot.
- **Anchor-owned state** is always asserted: the app policy (or `ClearAppPolicy` when `Idle`) and Anchor's
  own DND rule (or its removal).
- **User-owned state** (ringer, DND outside Anchor's rule) is touched only when the session needs it or a
  restore is still pending. An `Idle` reconcile never overwrites a ringer the user changed by hand.
- **Trigger registrations** are desired state too: `SyncTriggerRegistrations(configuredTriggers)` makes
  `core:triggers` re-register alarms, geofences, activity transitions, presence observations and BLE scans
  (brief feature 14), from data in the command, so the adapter needs no `core:data` access.
- Pending timers and recovery alarms are re-scheduled for their instants.
Registered alarms are cleared on reboot, and a trigger time in the past fires immediately [1], so
re-scheduling an expired instant is avoided rather than relied on.

### 5. Time

- **Wall clock** (`java.time.Clock` → `SystemZoneClock`) for timers and anything shown to the user.
  `AlarmManager` RTC alarms are "wall clock time in UTC" [1], so an instant-based timer doesn't depend on
  the time zone. What *does* need recomputing on `ACTION_TIMEZONE_CHANGED` and across DST is local-time
  schedule windows (M3), which `Reconcile` covers. The wall clock "can be set by the user or the phone
  network" [2]. Moving it forward can end a timer early, which is accepted as honest friction, not a jail.
- **Tamper-sensitive durations** (the 24 h lost-anchor delay; the rolling emergency-unlock window) also
  use monotonic time: `SystemClock.elapsedRealtime()`, "milliseconds since boot, including time spent in
  sleep" [2], tagged with `Settings.Global.BOOT_COUNT` (API 24+) [3] to detect reboots. **Invariant: a
  reboot may pause the lost-anchor countdown but never resets it, and recovery always completes.**
  Monotonic time is accumulated per boot segment, and a wall-clock upper bound guarantees completion even
  if accounting is lost. The reducer gets `env.elapsedRealtime` and `env.bootCount`, and `EngineState`
  stores the monotonic stamps, so moving the wall clock forward cannot complete the delay. The exact
  scheme is #38.
- **UI gesture durations** (2 s hold-to-start, 10 s emergency hold) use the Compose frame clock in the UI
  and never reach the engine.

References (checked 2026-11-20):
[1] https://developer.android.com/reference/android/app/AlarmManager ·
[2] https://developer.android.com/reference/android/os/SystemClock ·
[3] https://developer.android.com/reference/android/provider/Settings.Global#BOOT_COUNT ·
`ACTION_TIME_CHANGED` / `ACTION_TIMEZONE_CHANGED`: https://developer.android.com/reference/android/content/Intent

## Consequences

- Every rule, including restore and reconciliation, is a table-driven unit test on `reduce`.
- Process death at any point is safe: state and pending commands are persisted together, and executors
  dedupe.
- Adapters stay thin and depend only on `core:engine` ports. `core:data` owns persistence and ordering.
- The reducer's `when` over `Input` must stay exhaustive, so the compiler points at every new decision.
- Cost: an outbox table and command ids. Accepted for correctness.

## Alternatives considered

- **State machines inside Android services:** untestable without devices, and they lose state on process
  death.
- **A `Mutex` around reduce + execute:** not reentrant (failure callbacks deadlock), or, if released
  before executing, effects from consecutive transitions can interleave.
- **Event sourcing (persist inputs, rebuild state):** replay is deterministic if each stored input keeps
  its `now`. The real costs are that a reducer change reinterprets old history, plus input-schema
  evolution. Persisting state plus an outbox is simpler; session events for insights are recorded
  separately.
