package io.github.uveshm003.anchor.lint

import com.android.tools.lint.checks.infrastructure.TestFiles.java
import com.android.tools.lint.checks.infrastructure.TestFiles.kotlin
import com.android.tools.lint.checks.infrastructure.TestLintTask
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.android.tools.lint.client.api.LintClient
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SystemTimeDetectorTest {
    private fun lintTask(): TestLintTask = lint().allowMissingSdk().issues(SystemTimeDetector.ISSUE)

    @Test
    fun `reports a planted Instant now call`() {
        lintTask()
            .files(
                kotlin(
                    """
                    package test.pkg

                    import java.time.Instant

                    class Session {
                        fun start(): Instant = Instant.now()
                    }
                    """,
                ).indented(),
            )
            .run()
            .expect(
                """
                src/test/pkg/Session.kt:6: Error: Read time from the injected Clock, not Instant.now() [SystemTimeCall]
                    fun start(): Instant = Instant.now()
                                           ~~~~~~~~~~~~~
                1 error
                """,
            )
    }

    @Test
    fun `reports every system clock and default zone read`() {
        lintTask()
            .files(
                kotlin(
                    """
                    package test.pkg

                    import java.time.Clock
                    import java.time.Instant
                    import java.time.LocalDate
                    import java.time.LocalDateTime
                    import java.time.ZoneId
                    import java.time.ZoneOffset
                    import java.time.ZonedDateTime
                    import java.util.TimeZone

                    class Banned {
                        fun millis() = System.currentTimeMillis()
                        fun instant() = Instant.now()
                        fun date() = LocalDate.now()
                        fun dateTime() = LocalDateTime.now()
                        fun zoned() = ZonedDateTime.now()
                        fun zonedIn(zone: ZoneId) = ZonedDateTime.now(zone)
                        fun defaultZoneClock() = Clock.systemDefaultZone()
                        fun utcClock() = Clock.systemUTC()
                        fun zoneClock() = Clock.system(ZoneOffset.UTC)
                        fun tickingClock() = Clock.tickSeconds(ZoneOffset.UTC)
                        fun zone() = ZoneId.systemDefault()
                        fun timeZone() = TimeZone.getDefault()
                    }
                    """,
                ).indented(),
                java(
                    """
                    package test.pkg;

                    class JavaBanned {
                        long millis() {
                            return System.currentTimeMillis();
                        }
                    }
                    """,
                ).indented(),
            )
            .run()
            .expectErrorCount(BANNED_CALLS)
    }

    @Test
    fun `allows reads through an injected Clock`() {
        lintTask()
            .files(
                kotlin(
                    """
                    package test.pkg

                    import java.time.Clock
                    import java.time.Instant
                    import java.time.LocalDate
                    import java.time.ZoneId

                    class Allowed(private val clock: Clock) {
                        fun instant(): Instant = Instant.now(clock)
                        fun date(): LocalDate = LocalDate.now(clock)
                        fun millis(): Long = clock.millis()
                        fun zone(): ZoneId = clock.zone
                        fun fixed(): Clock = Clock.fixed(Instant.EPOCH, ZoneId.of("UTC"))
                        fun nanos(): Long = System.nanoTime()
                    }
                    """,
                ).indented(),
            )
            .run()
            .expectClean()
    }

    @Test
    fun `allows SystemZoneClock only in its own package`() {
        lintTask()
            .files(
                kotlin(
                    """
                    package io.github.uveshm003.anchor.core.common.time

                    import java.time.Clock
                    import java.time.Instant
                    import java.time.ZoneId

                    object SystemZoneClock : Clock() {
                        override fun getZone(): ZoneId = ZoneId.systemDefault()
                        override fun withZone(zone: ZoneId): Clock = system(zone)
                        override fun instant(): Instant = Instant.now()
                        override fun millis(): Long = System.currentTimeMillis()
                    }
                    """,
                ).indented(),
                kotlin(
                    """
                    package test.pkg

                    import java.time.Instant

                    object SystemZoneClock {
                        fun instant(): Instant = Instant.now()
                    }
                    """,
                ).indented(),
            )
            .run()
            .expectErrorCount(1)
    }

    @Test
    fun `registry exposes the issue`() {
        LintClient.clientName = LintClient.CLIENT_UNIT_TESTS
        val registry = AnchorIssueRegistry()

        assertThat(registry.issues).containsExactly(SystemTimeDetector.ISSUE)
        assertThat(registry.vendor.identifier).isEqualTo("anchor-lint-rules")
    }

    private companion object {
        /** 12 in the Kotlin file (`ZonedDateTime.now(zone)` reads the system clock too) + 1 in the Java file. */
        const val BANNED_CALLS = 13
    }
}
