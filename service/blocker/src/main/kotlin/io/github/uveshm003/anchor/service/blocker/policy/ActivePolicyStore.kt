package io.github.uveshm003.anchor.service.blocker.policy

import io.github.uveshm003.anchor.core.engine.Command
import io.github.uveshm003.anchor.core.engine.CommandExecutor
import io.github.uveshm003.anchor.core.engine.ExecutionResult
import io.github.uveshm003.anchor.core.model.AppPolicy
import io.github.uveshm003.anchor.core.model.ExitRule
import io.github.uveshm003.anchor.core.model.SessionId
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** What the blocker enforces and the block screen shows. Essentials are already removed by the engine. */
data class ActivePolicy(
    val sessionId: SessionId,
    val modeName: String,
    val policy: AppPolicy,
    val startedAt: Instant,
    val exitRule: ExitRule,
    val endsAt: Instant?,
    val requiredAnchorName: String?,
)

/**
 * The policy in force, in memory. After process death the controller's app-start reconcile re-applies it before
 * anything else (ADR 0003 §4), so it doesn't need its own persistence.
 */
@Singleton
class ActivePolicyStore @Inject constructor() {
    private val state = MutableStateFlow<ActivePolicy?>(null)
    val active: StateFlow<ActivePolicy?> = state

    fun set(policy: ActivePolicy?) {
        state.value = policy
    }
}

/** Executes `ApplyAppPolicy` / `ClearAppPolicy` (idempotent: applying the same command twice is harmless). */
class PolicyExecutor @Inject constructor(private val store: ActivePolicyStore) : CommandExecutor {
    override val handles = setOf(Command.ApplyAppPolicy::class, Command.ClearAppPolicy::class)

    override suspend fun execute(command: Command): ExecutionResult {
        when (command) {
            is Command.ApplyAppPolicy -> store.set(
                ActivePolicy(
                    sessionId = command.sessionId,
                    modeName = command.modeName,
                    policy = command.policy,
                    startedAt = command.startedAt,
                    exitRule = command.exitRule,
                    endsAt = command.endsAt,
                    requiredAnchorName = command.requiredAnchorName,
                ),
            )

            is Command.ClearAppPolicy -> store.set(null)

            else -> return ExecutionResult.Failed("Not a policy command: ${command::class.simpleName}")
        }
        return ExecutionResult.Done
    }
}
