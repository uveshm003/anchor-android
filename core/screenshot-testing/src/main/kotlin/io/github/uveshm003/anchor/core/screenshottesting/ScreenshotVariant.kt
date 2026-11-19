package io.github.uveshm003.anchor.core.screenshottesting

/**
 * Devices every screen is captured on (brief §3: phone and tablet). Every size-related qualifier is explicit
 * (smallest width, width, height, screen size, aspect, orientation): Robolectric only derives `sw`/size/long
 * when they are unset, so a relative qualifier would otherwise keep its defaults or a class-level `@Config`.
 */
enum class ScreenshotDevice(internal val qualifiers: String, internal val density: String) {
    Phone(qualifiers = "sw411dp-w411dp-h891dp-normal-long-port", density = "xxhdpi"),
    Tablet(qualifiers = "sw800dp-w1280dp-h800dp-xlarge-notlong-land", density = "xhdpi"),
}

/** One screenshot configuration. Its [toString] names the JUnit test and the golden file suffix. */
data class ScreenshotVariant(val device: ScreenshotDevice, val darkTheme: Boolean) {
    /**
     * Relative (`+`) qualifiers that set the whole screen configuration and night mode, leaving everything else
     * (notably a class-level `@Config(qualifiers = "hi")` locale) untouched.
     */
    val qualifiers: String
        get() = "+${device.qualifiers}-${if (darkTheme) "night" else "notnight"}-${device.density}"

    override fun toString(): String = "${device.name.lowercase()}_${if (darkTheme) "dark" else "light"}"

    companion object {
        fun all(devices: List<ScreenshotDevice> = ScreenshotDevice.entries): List<ScreenshotVariant> =
            devices.flatMap { device -> listOf(false, true).map { dark -> ScreenshotVariant(device, dark) } }

        /** For `@ParameterizedRobolectricTestRunner.Parameters(name = "{0}")`. */
        fun parameters(devices: List<ScreenshotDevice> = ScreenshotDevice.entries): List<Array<Any>> =
            all(devices).map { arrayOf(it) }
    }
}
