# 9. QR anchors: ZXing core, offline, instead of ML Kit

Date: 2026-11-26 · Status: Accepted · Issues: #40, #41

## Context

QR anchors need a QR encoder (to print the code) and a decoder (to scan it with the camera). The backlog
suggested ML Kit barcode scanning for decoding, with the rule that if it adds `INTERNET` we stop and use an offline
decoder instead (brief §8: Anchor never has `INTERNET`).

Checked 2026-11-26: adding `com.google.mlkit:barcode-scanning:17.3.0` to `app` puts
`android.permission.INTERNET` and `ACCESS_NETWORK_STATE` into the merged release manifest (its data-transport
dependency), which `checkReleaseNoInternetPermission` would reject.

## Decision

Use **ZXing `core` 3.5.4** (pure Java, no manifest, no network) for both encoding and decoding, fed by CameraX
1.7.0 `ImageAnalysis` frames (luminance plane). No ML Kit, so no `INTERNET` and no stripping of a dependency's
permissions.

## Consequences

- QR anchors work fully offline; the privacy check stays green.
- ZXing is slower and less tolerant than ML Kit on blurry or angled codes. Printed anchors are high-contrast and
  large, which suits it; `TRY_HARDER` is on.
- The secret is 32 random bytes on the code; only its salted hash is stored (ADR 0008), so a lost printout means
  making a new QR anchor.
