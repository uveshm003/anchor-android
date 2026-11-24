package io.github.uveshm003.anchor.core.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import io.github.uveshm003.anchor.core.common.time.TestClock
import io.github.uveshm003.anchor.core.data.platform.DeviceUptime
import io.github.uveshm003.anchor.core.data.repository.OfflineAnchorRepository
import io.github.uveshm003.anchor.core.data.repository.OfflineModeRepository
import io.github.uveshm003.anchor.core.data.repository.QuotaChange
import io.github.uveshm003.anchor.core.data.repository.SettingsRepository
import io.github.uveshm003.anchor.core.data.repository.UserSettings
import io.github.uveshm003.anchor.core.data.session.EngineJson
import io.github.uveshm003.anchor.core.data.session.EngineStore
import io.github.uveshm003.anchor.core.data.session.EnvironmentSampler
import io.github.uveshm003.anchor.core.data.session.SessionController
import io.github.uveshm003.anchor.core.database.AnchorDatabase
import io.github.uveshm003.anchor.core.database.entity.OutboxEntity
import io.github.uveshm003.anchor.core.database.entity.OutboxStatus
import io.github.uveshm003.anchor.core.engine.Command
import io.github.uveshm003.anchor.core.engine.CommandExecutor
import io.github.uveshm003.anchor.core.engine.ExecutionResult
import io.github.uveshm003.anchor.core.engine.Input
import io.github.uveshm003.anchor.core.engine.Outcome
import io.github.uveshm003.anchor.core.model.AppPolicy
import io.github.uveshm003.anchor.core.model.ExitRule
import io.github.uveshm003.anchor.core.model.Mode
import io.github.uveshm003.anchor.core.model.ModeId
import io.github.uveshm003.anchor.core.model.SessionEnd
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionControllerTest {
    private val db = inMemoryDb()
    private val clock = TestClock()
    private val executed = mutableListOf<Command>()
    private var failNext: KClass<out Command>? = null

    private val executor = object : CommandExecutor {
        override val handles = SessionController.COMMAND_TYPES

        override suspend fun execute(command: Command): ExecutionResult {
            if (command::class == failNext) {
                failNext = null
                return ExecutionResult.Failed("boom")
            }
            executed += command
            return ExecutionResult.Done
        }
    }

    private val settings = object : SettingsRepository {
        override val settings = MutableStateFlow(UserSettings(true, 3, true, null))

        override suspend fun setOnboardingCompleted(completed: Boolean) = Unit

        override suspend fun setDefaultMode(id: ModeId?) = Unit

        override suspend fun setAllowDefaultSmsApp(allow: Boolean) = Unit

        override suspend fun setEmergencyQuota(perWeek: Int) = QuotaChange.Changed
    }

    @After
    fun close() = db.close()

    private fun TestScope.controller(database: AnchorDatabase = db) = SessionController(
        scope = backgroundScope,
        environment = EnvironmentSampler(
            clock = clock,
            modes = OfflineModeRepository(database.modeDao()),
            anchors = OfflineAnchorRepository(database.anchorDao()),
            settings = settings,
            essentialApps = { setOf("com.android.dialer") },
            uptime = object : DeviceUptime {
                override fun elapsedRealtimeMillis() = 0L

                override fun bootCount() = 1
            },
        ),
        store = EngineStore(database.transactions(), database.engineDao(), database.sessionDao()),
        executors = setOf(executor),
    ).also { it.start() }

    private suspend fun addMode(exitRule: ExitRule = ExitRule.RequiresAnchor()): ModeId =
        OfflineModeRepository(db.modeDao())
            .upsert(
                Mode(
                    ModeId(0),
                    "Work",
                    appPolicy = AppPolicy.BlockList(setOf("com.social", "com.android.dialer")),
                    exitRule = exitRule,
                ),
            )

    @Test
    fun start_reconciles_idle_then_hold_persists_and_executes_in_order() = runTest {
        val modeId = addMode(ExitRule.TimerEnds(25))
        val controller = controller()

        val outcome = controller.request(Input.HoldToStart(modeId))

        assertThat(outcome).isInstanceOf(Outcome.Started::class.java)
        assertThat(executed.map { it::class }).containsExactly(
            Command.ClearAppPolicy::class,
            Command.HideSessionNotification::class,
            Command.ApplyAppPolicy::class,
            Command.ShowSessionNotification::class,
            Command.ScheduleTimer::class,
        ).inOrder()
        val apply = executed.filterIsInstance<Command.ApplyAppPolicy>().single()
        assertThat(apply.policy).isEqualTo(AppPolicy.BlockList(setOf("com.social")))
        assertThat(db.engineDao().pendingCommands()).isEmpty()
        assertThat(EngineJson.decodeState(db.engineDao().getState()!!.json).active?.session?.modeId).isEqualTo(modeId)
        assertThat(db.sessionDao().observeActive().first()?.modeName).isEqualTo("Work")
    }

    @Test
    fun emergency_unlock_ends_and_logs_the_unlock() = runTest {
        val modeId = addMode()
        val controller = controller()
        controller.request(Input.HoldToStart(modeId))

        val outcome = controller.request(Input.EmergencyUnlock) as Outcome.Ended

        assertThat(outcome.session.endedBy).isEqualTo(SessionEnd.EmergencyUnlock)
        assertThat(db.sessionDao().observeUnlockCountSince(0).first()).isEqualTo(1)
        assertThat(executed.takeLast(2).map { it::class })
            .containsExactly(Command.ClearAppPolicy::class, Command.HideSessionNotification::class).inOrder()
    }

    @Test
    fun a_failed_command_is_marked_failed_and_reduced_before_the_next_input() = runTest {
        val modeId = addMode()
        val controller = controller()
        failNext = Command.ApplyAppPolicy::class

        controller.request(Input.HoldToStart(modeId))

        assertThat(EngineJson.decodeState(db.engineDao().getState()!!.json).degraded).isTrue()
        assertThat(db.engineDao().pendingCommands()).isEmpty()
        // The next input still runs normally.
        assertThat(controller.request(Input.StopRequested)).isInstanceOf(Outcome.Rejected::class.java)
    }

    @Test
    fun a_new_controller_on_the_same_db_reruns_pending_commands_and_restores_the_session() = runTest {
        val modeId = addMode()
        controller().request(Input.HoldToStart(modeId))
        // Simulate a crash after persisting a transition but before executing it.
        db.engineDao().insertCommands(
            listOf(
                OutboxEntity(
                    "pending-1",
                    1_000,
                    EngineJson.encodeCommand(Command.ClearAppPolicy("pending-1")),
                    OutboxStatus.Pending,
                ),
            ),
        )
        executed.clear()

        val restarted = controller()
        restarted.request(Input.StopRequested)

        assertThat(executed.first()).isEqualTo(Command.ClearAppPolicy("pending-1"))
        // App-start reconcile re-applies the still-active session.
        assertThat(executed.filterIsInstance<Command.ApplyAppPolicy>()).hasSize(1)
    }

    @Test
    fun registry_fails_fast_on_missing_or_duplicate_executors() {
        val partial = object : CommandExecutor {
            override val handles = setOf(Command.ClearAppPolicy::class)

            override suspend fun execute(command: Command) = ExecutionResult.Done
        }
        assertThrows(IllegalStateException::class.java) { SessionController.buildRegistry(setOf(partial)) }
        assertThrows(IllegalStateException::class.java) { SessionController.buildRegistry(setOf(executor, partial)) }
        assertThat(SessionController.buildRegistry(setOf(executor))).hasSize(SessionController.COMMAND_TYPES.size)
    }
}
