package io.github.uveshm003.anchor.core.designsystem.component

/** What sits at the end of an [AnchorListRow]. */
sealed interface ListRowTrailing {
    data object None : ListRowTrailing

    /** Navigates somewhere: the whole row is a button. */
    data object Chevron : ListRowTrailing

    /** A setting: the whole row toggles, announced as one switch. */
    data class Switch(val checked: Boolean, val onCheckedChange: (Boolean) -> Unit) : ListRowTrailing
}
