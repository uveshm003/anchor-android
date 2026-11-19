package io.github.uveshm003.anchor.core.screenshottesting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config

/** A class-level Hindi locale survives, and each variant reaches the composition. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(qualifiers = "hi")
class ScreenshotRuleTest(private val variant: ScreenshotVariant) {
    @get:Rule
    val screenshot = ScreenshotRule(variant)

    @Test
    fun configurationMatchesTheVariant() = assertConfigurationMatches(screenshot, expectedLanguage = "hi")

    @Test
    fun goldenPath_includesANonEnglishLocale() {
        assertEquals("src/test/screenshots/home_hi_$variant.png", screenshot.goldenPath("home"))
    }

    @Test
    fun secondRenderInOneTest_fails() {
        screenshot.render { }
        assertThrows(IllegalStateException::class.java) { screenshot.render { } }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants() = ScreenshotVariant.parameters()
    }
}

/** Conflicting class-level screen qualifiers must not leak into any variant. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(qualifiers = "sw600dp-w700dp-h700dp-large-long-land")
class ScreenshotRuleIsolationTest(private val variant: ScreenshotVariant) {
    @get:Rule
    val screenshot = ScreenshotRule(variant)

    @Test
    fun classLevelScreenQualifiers_doNotLeak() = assertConfigurationMatches(screenshot, expectedLanguage = "en")

    @Test
    fun goldenPath_hasNoLocalePartForEnglish() {
        assertEquals("src/test/screenshots/home_$variant.png", screenshot.goldenPath("home"))
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun variants() = ScreenshotVariant.parameters()
    }
}
