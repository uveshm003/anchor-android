package io.github.uveshm003.anchor.core.engine

import io.github.uveshm003.anchor.core.model.AppPolicy
import io.github.uveshm003.anchor.core.model.UnlockRecord
import java.time.Duration
import java.time.Instant

/** Essentials (dialer, emergency, launcher, system UI, Anchor, the SMS app if allowed) are never blocked. */
object EssentialsPolicy {
    fun apply(policy: AppPolicy, essentials: Set<String>): AppPolicy = when (policy) {
        is AppPolicy.BlockList -> AppPolicy.BlockList(policy.packages - essentials)
        is AppPolicy.AllowList -> AppPolicy.AllowList(policy.packages + essentials)
    }
}

/**
 * Emergency unlocks per rolling 7 days. An unlock counts while `at > now - 7 days`; unlocks dated after `now`
 * (the clock was moved back) still count, so moving the clock back never frees quota.
 */
object EmergencyQuota {
    val WINDOW: Duration = Duration.ofDays(7)

    fun counted(unlocks: List<UnlockRecord>, now: Instant): List<UnlockRecord> =
        unlocks.filter { it.at.isAfter(now.minus(WINDOW)) }

    fun remaining(unlocks: List<UnlockRecord>, now: Instant, perWeek: Int): Int =
        (perWeek - counted(unlocks, now).size).coerceAtLeast(0)

    /** When the oldest counted unlock leaves the window (only meaningful when [remaining] is 0). */
    fun nextAvailableAt(unlocks: List<UnlockRecord>, now: Instant): Instant =
        counted(unlocks, now).minOfOrNull { it.at }?.plus(WINDOW) ?: now
}
