package io.github.uveshm003.anchor.core.designsystem.component

/** The optional button of an [EmptyState]: label and handler always come together. */
data class EmptyStateAction(val label: String, val onClick: () -> Unit)
