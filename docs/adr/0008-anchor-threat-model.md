# 8. Anchor threat model

Date: 2026-11-24 · Status: Accepted · Issue: #29

## Context

An anchor is an NFC sticker (or, from #40/#41, a printed QR code) that starts and ends focus sessions. Anchor is a
self-control tool: the person most likely to "attack" it is its own user, trying to end a session without walking
to the anchor. It is on-device only (no INTERNET), so there is no remote attacker or server to protect.

## Threats and decisions

| Threat | Decision |
|---|---|
| **Cloning a tag** (copying the UID with an NFC tool or emulating it with a second phone) | **Accepted.** Effort is the point: someone who goes that far has made a deliberate choice. Mitigation is only that the UID is never shown in the app. |
| **Photographing a QR anchor** (#41) | **Accepted**, same reasoning. The QR secret is random and app-generated, not guessable. |
| **Another app forging an "anchor tap"** (sending `NDEF_DISCOVERED` with Anchor's URI to `AnchorTapActivity`) | **Prevented.** The URI never counts as a tap: only the `Tag` object's UID does, hashed with the per-install salt and matched against paired anchors. An intent without a real tag does nothing; an unknown hash is ignored by the engine. |
| **Debug tap simulator** | Debug builds only (`src/debug`), protected by `android.permission.DUMP` (held by `adb shell`, not grantable to apps). Not in release builds. |
| **Reading stored anchors** | Only `SHA-256(salt ‖ UID)` is stored (`AnchorHasher`). The salt is per install (DataStore, `SecureRandom`, 32 bytes). Backups are off (`allowBackup="false"`), so the salt and hashes don't leave the device. |
| **Reinstall** | A reinstall makes a new salt, so old anchors must be paired again. Accepted: the app also forgets its modes and sessions, so there is no session to escape. |
| **Lost or broken anchor** | Never a lock-out: emergency unlock (3 per rolling week), lost-anchor recovery (24 h, ADR 0007), and, without strict mode, turning off the Accessibility service in system settings. An unpaired specific anchor falls back to any anchor. |

## Rules that code reviews check

1. Nothing ever treats the NDEF URI, a QR payload's format, or an intent extra other than a system `Tag` as proof of
   a tap; only a salted hash that matches a paired anchor does.
2. Tag UIDs and QR secrets are hashed immediately (`AnchorTags`) and never stored, logged or shown.
3. The tap simulator and any other test hook exist only in debug source sets and need `DUMP`.
4. Every way to require an anchor keeps a way out (emergency unlock, recovery, fallback to any anchor).
5. The salt never leaves the device (no backup, no export).
