package io.github.uveshm003.anchor.core.testing.screenshot

/** Devices every screen is captured on (brief §3: phone and tablet). */
enum class ScreenshotDevice(internal val size: String, internal val density: String) {
    Phone(size = "w411dp-h891dp", density = "xxhdpi"),
    Tablet(size = "w1280dp-h800dp", density = "xhdpi"),
}
