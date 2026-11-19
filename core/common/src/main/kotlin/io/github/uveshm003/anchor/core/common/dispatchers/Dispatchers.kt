package io.github.uveshm003.anchor.core.common.dispatchers

import javax.inject.Qualifier

/**
 * Qualifies an injected [kotlinx.coroutines.CoroutineDispatcher], e.g. `@Dispatcher(AnchorDispatchers.IO)`.
 * Tests construct classes with test dispatchers instead.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val dispatcher: AnchorDispatchers)

enum class AnchorDispatchers { Default, IO }

/** Process-wide scope for work that must outlive a screen (e.g. persisting a session). Uses `SupervisorJob`. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope
