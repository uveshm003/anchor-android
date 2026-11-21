package io.github.uveshm003.anchor.core.designsystem.component

/**
 * What an [AnchorListRow] does and shows at its end. The action lives on the variant, so a row can't show a
 * navigation arrow without an action, or drop a click on a switch row.
 */
sealed interface ListRowTrailing {
    /** Static information; not focusable as a control. */
    data object None : ListRowTrailing

    /** Performs an action in place (no arrow): the whole row is a button. */
    data class Clickable(val onClick: () -> Unit) : ListRowTrailing

    /** Navigates somewhere: the whole row is a button with a trailing arrow. */
    data class Chevron(val onClick: () -> Unit) : ListRowTrailing

    /** A setting: the whole row toggles and is announced as one switch. */
    data class Switch(val checked: Boolean, val onCheckedChange: (Boolean) -> Unit) : ListRowTrailing
}
