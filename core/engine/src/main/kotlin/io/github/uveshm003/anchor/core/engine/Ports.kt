package io.github.uveshm003.anchor.core.engine

import io.github.uveshm003.anchor.core.model.SessionId
import kotlin.reflect.KClass

/** Adapters (NFC, triggers, services, UI) submit inputs here; `core:data`'s controller implements it (ADR 0003). */
interface SessionInputs {
    suspend fun submit(input: Input)
}

/**
 * Carries out one kind of [Command]. Exactly one executor handles each command type; executors are idempotent
 * and dedupe by command id. Adapters provide them with Hilt `@IntoSet`.
 */
interface CommandExecutor {
    val handles: Set<KClass<out Command>>

    suspend fun execute(command: Command): ExecutionResult
}

sealed interface ExecutionResult {
    data object Done : ExecutionResult

    data class Failed(val reason: String) : ExecutionResult
}

/** Turns a tag UID or QR secret into the stored anchor hash (salted SHA-256, hex). Implemented in `core:data`. */
fun interface AnchorHasher {
    suspend fun hash(secret: ByteArray): String
}

/** Records that a blocked app was opened during a session (for insights). Implemented in `core:data`. */
fun interface BlockAttemptRecorder {
    suspend fun record(sessionId: SessionId, packageName: String)
}
