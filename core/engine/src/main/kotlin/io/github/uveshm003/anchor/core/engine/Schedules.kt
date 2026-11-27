package io.github.uveshm003.anchor.core.engine

import io.github.uveshm003.anchor.core.model.Mode
import io.github.uveshm003.anchor.core.model.StartTrigger
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Weekly schedule windows in the user's current time zone (pure). A window opens on each listed day at the start
 * time and closes at the end time, the next day when it crosses midnight. Local times that don't exist (spring
 * forward) move forward with the clock; repeated ones (fall back) use the first occurrence (java.time rules).
 */
object Schedules {
    private const val LOOKBACK_DAYS = 1L
    private const val LOOKAHEAD_DAYS = 8L
    private const val SECONDS_PER_MINUTE = 60L

    data class Window(val start: Instant, val end: Instant)

    /** Windows that start between yesterday and a week from [around]. */
    fun windows(schedule: StartTrigger.Schedule, around: Instant, zone: ZoneId): List<Window> {
        val today = around.atZone(zone).toLocalDate()
        return (-LOOKBACK_DAYS..LOOKAHEAD_DAYS).map(today::plusDays)
            .filter { it.dayOfWeek.value in schedule.days }
            .map { date ->
                val endDate = if (schedule.endMinute > schedule.startMinute) date else date.plusDays(1)
                Window(
                    start = ZonedDateTime.of(date, time(schedule.startMinute), zone).toInstant(),
                    end = ZonedDateTime.of(endDate, time(schedule.endMinute), zone).toInstant(),
                )
            }
    }

    fun isOpen(schedule: StartTrigger.Schedule, now: Instant, zone: ZoneId): Boolean =
        windows(schedule, now, zone).any { !now.isBefore(it.start) && now.isBefore(it.end) }

    /** The next instant after [now] at which this schedule opens or closes. */
    fun nextChange(schedule: StartTrigger.Schedule, now: Instant, zone: ZoneId): Instant? =
        windows(schedule, now, zone).flatMap { listOf(it.start, it.end) }.filter { it.isAfter(now) }.minOrNull()

    /** Schedule triggers that are true now: one per mode with an open window. */
    fun openKeys(modes: List<Mode>, now: Instant, zone: ZoneId): Set<TriggerKey> = modes
        .filter { mode -> mode.scheduleTriggers().any { isOpen(it, now, zone) } }
        .map { TriggerKey.Schedule(it.id) }
        .toSet()

    /** When to wake up next to re-check schedules, or null if no mode has one. */
    fun nextWake(modes: List<Mode>, now: Instant, zone: ZoneId): Instant? =
        modes.flatMap { it.scheduleTriggers() }.mapNotNull { nextChange(it, now, zone) }.minOrNull()

    private fun Mode.scheduleTriggers() = startTriggers.filterIsInstance<StartTrigger.Schedule>()

    private fun time(minuteOfDay: Int): LocalTime = LocalTime.ofSecondOfDay(minuteOfDay * SECONDS_PER_MINUTE)
}
