# 4. Navigation: Navigation Compose 2.x type-safe routes

Date: 2026-11-23 · Status: Accepted · Issue: #3

## Context

The app needs top-level destinations owned by feature modules, back handling (including predictive back) and a
graph that `app` wires without features depending on each other. Two stable options (checked 2026-11-23):

| | Navigation Compose 2.10.2 | Navigation 3 1.2.2 |
|---|---|---|
| Routes | `@Serializable` classes, `composable<Route>` | Any key type; the app owns a `SnapshotStateList` back stack |
| Back stack | Owned by `NavController` (saved/restored for us) | Owned by the app; `rememberNavBackStack` for saving |
| Multi-pane | Not built in | Scenes (list-detail) built in |
| Brief | "type-safe Navigation Compose" (§3) | Used by the current official app template |
| In the catalog | Yes | No |

## Decision

Use **Navigation Compose 2.10 type-safe routes**. Each feature exposes a `@Serializable` route and a
`NavGraphBuilder.<feature>Screen()` entry; `app` builds the `NavHost` and the `NavigationSuiteScaffold`
(material3-adaptive-navigation-suite from the Compose BOM). Top-level navigation pops to the start destination
with `saveState`/`restoreState`, so each tab keeps its state and Back returns Home.

## Consequences

- Matches the brief and is already in the catalog: no new dependency or back-stack code to own while building
  the MVP.
- Predictive back comes from `NavHost` with `android:enableOnBackInvokedCallback="true"`.
- If list-detail layouts become important (e.g. modes on tablets), revisit Navigation 3; the feature-owned
  route + entry pattern keeps that migration local to `app` and each feature's `navigation` package.
