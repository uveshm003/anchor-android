package io.github.uveshm003.anchor.core.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.github.uveshm003.anchor.core.database.entity.AnchorEntity
import io.github.uveshm003.anchor.core.database.entity.BlockAttemptEntity
import io.github.uveshm003.anchor.core.database.entity.EmergencyUnlockEntity
import io.github.uveshm003.anchor.core.database.entity.EngineStateEntity
import io.github.uveshm003.anchor.core.database.entity.ModeEntity
import io.github.uveshm003.anchor.core.database.entity.OutboxEntity
import io.github.uveshm003.anchor.core.database.entity.OutboxStatus
import io.github.uveshm003.anchor.core.database.entity.SessionEntity
import io.github.uveshm003.anchor.core.model.AnchorType
import io.github.uveshm003.anchor.core.model.AppPolicy
import io.github.uveshm003.anchor.core.model.ExitRule
import io.github.uveshm003.anchor.core.model.ModeColor
import io.github.uveshm003.anchor.core.model.ModeIcon
import io.github.uveshm003.anchor.core.model.QuietActions
import io.github.uveshm003.anchor.core.model.SessionEnd
import io.github.uveshm003.anchor.core.model.SessionSource
import io.github.uveshm003.anchor.core.model.StartTrigger
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnchorDatabaseTest {
    private val db = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AnchorDatabase::class.java,
    )
        .allowMainThreadQueries()
        .build()

    @get:Rule
    val migrations = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AnchorDatabase::class.java)

    @After
    fun close() = db.close()

    private fun mode(name: String, priority: Int) = ModeEntity(
        name = name,
        icon = ModeIcon.Focus,
        color = ModeColor.Teal,
        priority = priority,
        appPolicy = AppPolicy.BlockList(setOf("com.social")),
        quietActions = QuietActions(doNotDisturb = true),
        startTriggers = setOf(StartTrigger.Hold, StartTrigger.Schedule(setOf(1), 540, 1020)),
        exitRule = ExitRule.TimerEnds(25),
        strict = false,
    )

    @Test
    fun modes_insert_update_delete_and_observe_by_priority() = runTest {
        val dao = db.modeDao()
        val study = dao.upsert(mode("Study", priority = 1))
        val work = dao.upsert(mode("Work", priority = 0))

        dao.observeAll().test {
            assertThat(awaitItem().map { it.name }).containsExactly("Work", "Study").inOrder()

            dao.upsert(dao.get(study)!!.copy(name = "Exams"))
            assertThat(awaitItem().map { it.name }).containsExactly("Work", "Exams").inOrder()

            dao.reorder(listOf(study, work))
            assertThat(awaitItem().map { it.name to it.priority }).containsExactly("Exams" to 0, "Work" to 1).inOrder()

            dao.delete(work)
            assertThat(awaitItem().single().name).isEqualTo("Exams")
        }
        assertThat(dao.getAll().single()).isEqualTo(mode("Exams", priority = 0).copy(id = study))
    }

    @Test
    fun anchors_round_trip() = runTest {
        val dao = db.anchorDao()
        val id = dao.upsert(AnchorEntity(name = "Desk", type = AnchorType.Nfc, hash = "a".repeat(64), modeId = null))

        assertThat(dao.getAll().single().id).isEqualTo(id)
        dao.delete(id)
        assertThat(dao.getAll()).isEmpty()
    }

    @Test
    fun observes_the_active_session_history_attempts_and_unlocks() = runTest {
        val dao = db.sessionDao()
        val active =
            SessionEntity(1, 1, "Work", 1_000, SessionSource.Hold, ExitRule.RequiresAnchor(), null, null, null, 0)

        dao.observeActive().test {
            assertThat(awaitItem()).isNull()
            dao.upsert(active)
            assertThat(awaitItem()).isEqualTo(active)

            dao.insertBlockAttempt(BlockAttemptEntity(sessionId = 1, packageName = "com.social", at = 1_500))
            dao.incrementBlockAttempts(1)
            assertThat(awaitItem()?.blockAttempts).isEqualTo(1)

            dao.upsert(active.copy(endedAt = 2_000, endedBy = SessionEnd.EmergencyUnlock, blockAttempts = 1))
            assertThat(awaitItem()).isNull()
        }
        dao.observeHistory().test { assertThat(awaitItem().single().endedBy).isEqualTo(SessionEnd.EmergencyUnlock) }

        dao.insertUnlock(EmergencyUnlockEntity(sessionId = 1, at = 2_000))
        dao.observeUnlockCountSince(1_999).test { assertThat(awaitItem()).isEqualTo(1) }
        dao.observeUnlockCountSince(2_000).test { assertThat(awaitItem()).isEqualTo(0) }
    }

    @Test
    fun engine_state_and_outbox() = runTest {
        val dao = db.engineDao()
        assertThat(dao.getState()).isNull()
        dao.putState(EngineStateEntity(json = "{}"))
        dao.putState(EngineStateEntity(json = "{\"nextSessionId\":2}"))
        assertThat(dao.getState()?.json).isEqualTo("{\"nextSessionId\":2}")
        dao.observeState().test { assertThat(awaitItem()?.json).contains("2") }

        dao.insertCommands(
            listOf(
                OutboxEntity("b", sequence = 2, json = "{}", status = OutboxStatus.Pending),
                OutboxEntity("a", sequence = 1, json = "{}", status = OutboxStatus.Pending),
            ),
        )
        assertThat(dao.pendingCommands().map { it.commandId }).containsExactly("a", "b").inOrder()
        assertThat(dao.lastSequence()).isEqualTo(2)

        dao.setStatus("a", OutboxStatus.Done)
        dao.setStatus("b", OutboxStatus.Failed)
        assertThat(dao.pendingCommands()).isEmpty()
        dao.pruneDone(beforeSequence = 2)
        assertThat(dao.lastSequence()).isEqualTo(2)
    }

    @Test
    fun exported_schema_v1_opens_with_the_migration_helper() {
        migrations.createDatabase(TEST_DB, 1).use { sqlite ->
            sqlite.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'modes'").use {
                assertThat(it.count).isEqualTo(1)
            }
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
