package io.github.uveshm003.anchor.core.engine

import io.github.uveshm003.anchor.core.model.Anchor
import io.github.uveshm003.anchor.core.model.AppPolicy
import io.github.uveshm003.anchor.core.model.ExitRule
import io.github.uveshm003.anchor.core.model.InstantAsEpochMillis
import io.github.uveshm003.anchor.core.model.Mode
import io.github.uveshm003.anchor.core.model.ModeId
import io.github.uveshm003.anchor.core.model.Session
import io.github.uveshm003.anchor.core.model.SessionId
import io.github.uveshm003.anchor.core.model.UnlockRecord
import java.time.Duration
import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Everything the rules need, persisted as a whole after every input (ADR 0003). */
@Serializable
data class EngineState(
    val active: ActiveSession? = null,
    /** Emergency unlocks of the last 7 days (older ones are pruned); the quota is computed from these. */
    val unlocks: List<UnlockRecord> = emptyList(),
    val nextSessionId: Long = 1,
    /** A command for the active session failed; the next reconcile retries by recomputing desired state. */
    val degraded: Boolean = false,
    /** Lost-anchor recovery counting down for the active session (see [Recovery]). */
    val recovery: PendingRecovery? = null,
)

/**
 * A lost-anchor recovery request. [countedMillis] is monotonic time already counted, up to the stamp
 * ([stampElapsed] in boot [stampBoot]); time in a boot that ended before it was stamped is lost, so a reboot pauses
 * the countdown but never resets it. [requestedAt] bounds it by wall clock so it always completes.
 */
@Serializable
data class PendingRecovery(
    val sessionId: SessionId,
    @Serializable(with = InstantAsEpochMillis::class) val requestedAt: Instant,
    val countedMillis: Long,
    val stampBoot: Int,
    val stampElapsed: Long,
)

/** The running session plus the mode as it was when it started (later edits don't change a running session). */
@Serializable
data class ActiveSession(val session: Session, val mode: Mode)

/**
 * Facts sampled by the controller once per input, when it is dequeued: the time and the user's configuration.
 * [essentials] are resolved on the Android side (dialer, launcher, system UI, Anchor, the SMS app if allowed).
 */
data class Environment(
    val now: Instant,
    val modes: List<Mode> = emptyList(),
    val anchors: List<Anchor> = emptyList(),
    val defaultModeId: ModeId? = null,
    val essentials: Set<String> = emptySet(),
    val emergencyQuotaPerWeek: Int = DEFAULT_EMERGENCY_QUOTA,
    /** `SystemClock.elapsedRealtime()`: monotonic, includes deep sleep, resets on reboot. */
    val elapsedRealtimeMillis: Long = 0,
    /** `Settings.Global.BOOT_COUNT`, to tell boots apart. */
    val bootCount: Int = 0,
)

const val DEFAULT_EMERGENCY_QUOTA = 3

/** Everything that can happen to the engine. Inputs carry platform facts only; time comes from [Environment]. */
@Serializable
sealed interface Input {
    @Serializable
    @SerialName("hold")
    data class HoldToStart(val modeId: ModeId) : Input

    /** The user asked to stop from the app; allowed only if the exit rule allows it (it explains how otherwise). */
    @Serializable
    @SerialName("stop")
    data object StopRequested : Input

    @Serializable
    @SerialName("anchor")
    data class AnchorTapped(val hash: String) : Input

    @Serializable
    @SerialName("emergency")
    data object EmergencyUnlock : Input

    @Serializable
    @SerialName("timer")
    data class TimerElapsed(val sessionId: SessionId) : Input

    @Serializable
    @SerialName("failed")
    data class CommandFailed(val commandId: String, val sessionId: SessionId?) : Input

    @Serializable
    @SerialName("reconcile")
    data class Reconcile(val cause: ReconcileCause) : Input

    /** Lost anchor: end the active session after a 24 h delay, whatever its exit rule or the quota. */
    @Serializable
    @SerialName("recovery_requested")
    data object RecoveryRequested : Input

    @Serializable
    @SerialName("recovery_cancelled")
    data object RecoveryCancelled : Input

    @Serializable
    @SerialName("recovery_elapsed")
    data class RecoveryElapsed(val sessionId: SessionId) : Input
}

@Serializable
enum class ReconcileCause { AppStart, Boot, PackageReplaced, TimeChanged, TimeZoneChanged, PermissionGranted }

/** Side effects for the Android executors. Every command has a stable [id]; executors dedupe by it. */
@Serializable
sealed interface Command {
    val id: String

    /** Block per [policy] (essentials already applied). The rest is what the block screen shows. */
    @Serializable
    @SerialName("apply_policy")
    data class ApplyAppPolicy(
        override val id: String,
        val sessionId: SessionId,
        val modeName: String,
        val policy: AppPolicy,
        @Serializable(with = InstantAsEpochMillis::class) val startedAt: Instant,
        val exitRule: ExitRule,
        @Serializable(with = InstantAsEpochMillis::class) val endsAt: Instant?,
        /** Name of the anchor that ends the session when the exit rule needs a specific one. */
        val requiredAnchorName: String?,
    ) : Command

    @Serializable
    @SerialName("clear_policy")
    data class ClearAppPolicy(override val id: String) : Command

    @Serializable
    @SerialName("schedule_timer")
    data class ScheduleTimer(
        override val id: String,
        val sessionId: SessionId,
        @Serializable(with = InstantAsEpochMillis::class) val at: Instant,
    ) : Command

    @Serializable
    @SerialName("cancel_timer")
    data class CancelTimer(override val id: String, val sessionId: SessionId) : Command

    /** The ongoing "session is on" notification (what the block screen shows, in the shade). */
    @Serializable
    @SerialName("show_notification")
    data class ShowSessionNotification(
        override val id: String,
        val sessionId: SessionId,
        val modeName: String,
        @Serializable(with = InstantAsEpochMillis::class) val startedAt: Instant,
        val exitRule: ExitRule,
        @Serializable(with = InstantAsEpochMillis::class) val endsAt: Instant?,
        val requiredAnchorName: String?,
    ) : Command

    @Serializable
    @SerialName("hide_notification")
    data class HideSessionNotification(override val id: String) : Command

    /** Wake at `elapsedRealtime` [elapsedRealtimeAt] (monotonic) to check the recovery again. */
    @Serializable
    @SerialName("schedule_recovery")
    data class ScheduleRecovery(override val id: String, val sessionId: SessionId, val elapsedRealtimeAt: Long) :
        Command

    @Serializable
    @SerialName("cancel_recovery")
    data class CancelRecovery(override val id: String, val sessionId: SessionId) : Command
}

/** What happened, for the UI. Rejections and ignores are values, never exceptions. */
sealed interface Outcome {
    data class Started(val session: Session) : Outcome

    data class Ended(val session: Session) : Outcome

    data class Rejected(val reason: RejectReason) : Outcome

    data class Ignored(val reason: IgnoreReason) : Outcome

    data object Reconciled : Outcome

    data class Degraded(val commandId: String) : Outcome

    /** Recovery is counting down; the session ends after [remaining] more monotonic time. */
    data class RecoveryScheduled(val remaining: Duration) : Outcome

    data object RecoveryCancelled : Outcome
}

sealed interface RejectReason {
    data class AlreadyActive(val modeName: String) : RejectReason

    data object ModeNotFound : RejectReason

    /** The mode can't be started this way (its start triggers don't include it). */
    data object TriggerNotEnabled : RejectReason

    data object NoDefaultMode : RejectReason

    data object NotActive : RejectReason

    /** "Tap an anchor" ([anchorName] null) or "Tap your Desk anchor". */
    data class RequiresAnchor(val anchorName: String?) : RejectReason

    data class EndsWithTimer(val endsAt: Instant) : RejectReason

    data object EndsWithTrigger : RejectReason

    /** Quota used up; the next unlock frees up at [nextAvailableAt]. Lost-anchor recovery is the way out. */
    data class QuotaExhausted(val nextAvailableAt: Instant) : RejectReason

    data object RecoveryAlreadyPending : RejectReason
}

enum class IgnoreReason { UnknownAnchor, StaleTimer, StaleCommand, NoRecovery }

data class Transition(val state: EngineState, val commands: List<Command>, val outcome: Outcome)
