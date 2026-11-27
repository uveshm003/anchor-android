# 10. Process watchers run inside the blocker service (USB trigger)

Date: 2026-11-28 · Status: Accepted · Issues: #94 (and later #93)

## Context

Some trigger events only reach a **running process**. Checked on developer.android.com, 2026-11-28:

- `ACTION_POWER_CONNECTED` / `ACTION_POWER_DISCONNECTED` are not on the implicit-broadcast exceptions list. Apps
  targeting API 26+ can't receive them through manifest receivers.
- `UsbManager` documents host mode only ("Currently only host mode is supported in the public API"). A normal
  app has no public signal for "connected to a computer" in device mode.
- `BatteryManager.EXTRA_PLUGGED` tells `BATTERY_PLUGGED_USB` ("Power source is a USB port") from AC, wireless
  and dock. It is published in the sticky `ACTION_BATTERY_CHANGED`, which only context-registered receivers get.

The brief rules out polling and an always-on foreground service without an ADR. Anchor already has a long-lived
component: the Accessibility service that does the blocking (ADR 0006). It runs whenever the user has turned
blocking on, which every automatic mode needs anyway.

## Decision

- A pure port, `ProcessWatcher { start(); stop() }`, lives in `core:engine`. Adapters bind theirs with Hilt
  `@IntoSet`; `service:blocker` declares the set with `@Multibinds` and depends on nothing else.
- `BlockerService` starts every watcher in `onServiceConnected` and stops them in `onDestroy`. Watchers only
  submit engine inputs, so there's no logic in the service.
- **USB:** `UsbPowerWatcher` registers a context receiver for `ACTION_BATTERY_CHANGED`. Its first delivery is
  the current state, which also clears a stale trigger after a process restart. It submits
  `TriggerStarted/TriggerEnded(TriggerKey.Usb)` only when "plugged into a USB port" changes.
- The engine starts the best mode for a trigger that is already on when it reconciles. So adding the USB
  trigger to a mode while plugged in starts it as soon as the mode is saved.

## Consequences

- No new service, permission or polling. USB plug/unplug is event-driven.
- The USB trigger only works while blocking (the Accessibility service) is on; the editor says so.
- **Limitation:** some USB wall adapters without charger detection also report `BATTERY_PLUGGED_USB`, so they
  count as a computer. The editor says so. A PC that only charges at a low current still counts as USB.
- If the user turns blocking off, watchers stop. A plugged-in trigger then stays "on" in the engine until the
  next `ACTION_BATTERY_CHANGED` after blocking is turned back on.
- The Wi-Fi trigger (#93) can use the same mechanism for a context-registered `NetworkCallback` with
  `FLAG_INCLUDE_LOCATION_INFO`. Background location is still needed for SSID/BSSID; that gets its own ADR.
