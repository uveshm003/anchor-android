package io.github.uveshm003.anchor.core.designsystem.component

/** How accessibility services (TalkBack, Switch Access) complete a [HoldToConfirmButton] without a sustained touch. */
sealed interface AccessibleHold {
    /** A custom action confirms immediately. For actions where the hold is a convenience (hold-to-start). */
    data class Immediate(val label: String? = null) : AccessibleHold

    /**
     * A custom action starts a cancellable countdown of the full hold, announced in quarters. Keeps the friction
     * (e.g. the 10 s emergency hold) while staying reachable for users who can't hold.
     */
    data class Countdown(val label: String? = null) : AccessibleHold
}
