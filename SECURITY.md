# Security policy

## Reporting a vulnerability

Please **do not open a public issue** for security problems. Use GitHub's private reporting instead:
**Security → Report a vulnerability** on this repository. You should get a reply within 7 days.

Useful things to include: the Anchor version (Settings → About), Android version and device, steps to
reproduce, and the impact you expect.

## Scope

In scope:

- Ways for another app to start or stop a session, forge an anchor tap, or read Anchor's data
  (exported components, intents, `PendingIntent`s, content providers).
- Bypasses of the emergency-unlock quota or the lost-anchor recovery delay *by another app*.
- Anything that makes Anchor send data off the device. Anchor has no `INTERNET` permission by design.

Out of scope, by design (see `docs/adr/` threat model once written):

- Cloning an NFC tag or photographing a QR anchor. Anchor is a self-control tool, not access control.
- A user escaping their own session with safe mode, ADB or by uninstalling. Anchor adds friction; it
  does not pretend Android cannot be escaped.

## Supported versions

Only the latest release receives fixes.
