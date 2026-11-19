package io.github.uveshm003.anchor.core.screenshottesting

/** Devices every screen is captured on (brief §3: phone and tablet). Orientation is explicit so nothing leaks. */
enum class ScreenshotDevice(internal val size: String, internal val orientation: String, internal val density: String) {
    Phone(size = "w411dp-h891dp", orientation = "port", density = "xxhdpi"),
    Tablet(size = "w1280dp-h800dp", orientation = "land", density = "xhdpi"),
}

/** One screenshot configuration. Its [toString] names the JUnit test and the golden file suffix. */
data class ScreenshotVariant(val device: ScreenshotDevice, val darkTheme: Boolean) {
    /**
     * Relative (`+`) qualifiers: they change only size, orientation, night mode and density, so a class-level
     * `@Config(qualifiers = "hi")` locale is kept.
     */
    val qualifiers: String
        get() = "+${device.size}-${device.orientation}-${if (darkTheme) "night" else "notnight"}-${device.density}"

    override fun toString(): String = "${device.name.lowercase()}_${if (darkTheme) "dark" else "light"}"

    companion object {
        fun all(devices: List<ScreenshotDevice> = ScreenshotDevice.entries): List<ScreenshotVariant> =
            devices.flatMap { device -> listOf(false, true).map { dark -> ScreenshotVariant(device, dark) } }

        /** For `@ParameterizedRobolectricTestRunner.Parameters(name = "{0}")`. */
        fun parameters(devices: List<ScreenshotDevice> = ScreenshotDevice.entries): List<Array<Any>> =
            all(devices).map { arrayOf(it) }
    }
}
