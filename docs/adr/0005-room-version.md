# 5. Persistence: Room 2.8 with exported schemas

Date: 2026-11-23 · Status: Accepted · Issue: #10

## Context

Modes, anchors, sessions, block attempts, emergency unlocks, the engine-state snapshot and the command outbox
(ADR 0003) need a database. Two stable lines exist (checked 2026-11-23): **Room 2.8.5** (`androidx.room`) and
**Room 3.0.3** (`androidx.room3`, a new package built around `SQLiteDriver` and Kotlin Multiplatform).

## Decision

Use **Room 2.8.5** via KSP, with the `androidx.room` Gradle plugin exporting schemas to `core/database/schemas`
(committed). An `anchor.android.room` convention plugin wires KSP, schema export and `room-testing`, and adds the
schemas to unit-test assets so Robolectric tests can open any version with `MigrationTestHelper`.

- Android-only app: Room 3's multiplatform focus brings nothing yet, and its new package means fewer examples
  for migration testing. Room 2.8 works with KSP 2 and AGP 9 here (the build compiles and the schema exports).
- Structured rules (`AppPolicy`, `ExitRule`, start triggers, quiet actions) are JSON columns through
  kotlinx.serialization, so adding a trigger type needs no schema change. Instants are epoch millis UTC.
- The engine state is one JSON row, and pending commands are outbox rows; both are written in one transaction.

## Consequences

- Every schema change needs a version bump, a migration and a migration test; the v1 schema test is the template.
- Moving to Room 3 later is a package rename plus driver setup, done in one PR when it pays off.
