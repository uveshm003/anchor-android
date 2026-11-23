package io.github.uveshm003.anchor.core.model

import com.google.common.truth.Truth.assertThat
import java.time.Instant
import kotlinx.serialization.json.Json
import org.junit.Assert.assertThrows
import org.junit.Test

class ModelTest {
    private val hash = "a".repeat(64)
    private val start = Instant.parse("2026-11-23T10:00:00Z")

    @Test
    fun `mode validates name and priority`() {
        assertThrows(IllegalArgumentException::class.java) { Mode(ModeId(1), name = " ") }
        assertThrows(IllegalArgumentException::class.java) { Mode(ModeId(1), name = "Work", priority = -1) }
        val mode = Mode(ModeId(1), name = "Deep Work")
        assertThat(mode.exitRule).isEqualTo(ExitRule.RequiresAnchor())
        assertThat(mode.startTriggers).containsExactly(StartTrigger.Hold, StartTrigger.AnchorTap)
    }

    @Test
    fun `block list blocks listed apps and allow list blocks everything else`() {
        val block = AppPolicy.BlockList(setOf("com.social"))
        val allow = AppPolicy.AllowList(setOf("com.maps"))

        assertThat(block.blocks("com.social")).isTrue()
        assertThat(block.blocks("com.maps")).isFalse()
        assertThat(allow.blocks("com.social")).isTrue()
        assertThat(allow.blocks("com.maps")).isFalse()
    }

    @Test
    fun `start triggers validate their parameters`() {
        assertThrows(IllegalArgumentException::class.java) { StartTrigger.Schedule(emptySet(), 0, 60) }
        assertThrows(IllegalArgumentException::class.java) { StartTrigger.Schedule(setOf(8), 0, 60) }
        assertThrows(IllegalArgumentException::class.java) { StartTrigger.Schedule(setOf(1), 0, 1440) }
        assertThrows(IllegalArgumentException::class.java) { StartTrigger.Schedule(setOf(1), 60, 60) }
        assertThrows(IllegalArgumentException::class.java) { StartTrigger.BluetoothDevice("car") }
        assertThrows(IllegalArgumentException::class.java) { StartTrigger.WifiNetwork(" ") }
        assertThrows(IllegalArgumentException::class.java) { StartTrigger.WifiNetwork("Home", bssid = "x") }

        StartTrigger.Schedule(setOf(1, 7), startMinute = 22 * 60, endMinute = 7 * 60)
        StartTrigger.BluetoothDevice("00:11:22:AA:bb:cc")
        StartTrigger.WifiNetwork("Office", bssid = "00:11:22:33:44:55")
    }

    @Test
    fun `timer must be positive`() {
        assertThrows(IllegalArgumentException::class.java) { ExitRule.TimerEnds(0) }
        assertThat(ExitRule.TimerEnds(25).minutes).isEqualTo(25)
    }

    @Test
    fun `session validates its instants`() {
        val session = Session(
            id = SessionId(1),
            modeId = ModeId(1),
            modeName = "Work",
            startedAt = start,
            startedBy = SessionSource.Hold,
            exitRule = ExitRule.RequiresAnchor(),
        )
        assertThat(session.isActive).isTrue()
        assertThat(session.emergencyUnlockUsed).isFalse()

        assertThrows(IllegalArgumentException::class.java) { session.copy(endsAt = start) }
        assertThrows(IllegalArgumentException::class.java) { session.copy(endedAt = start.minusSeconds(1)) }
        assertThrows(IllegalArgumentException::class.java) { session.copy(endedAt = start) }
        assertThrows(IllegalArgumentException::class.java) { session.copy(blockAttempts = -1) }

        val ended = session.copy(endedAt = start, endedBy = SessionEnd.EmergencyUnlock)
        assertThat(ended.isActive).isFalse()
        assertThat(ended.emergencyUnlockUsed).isTrue()
    }

    @Test
    fun `anchor place and space validate`() {
        assertThrows(IllegalArgumentException::class.java) { Anchor(AnchorId(1), "", AnchorType.Nfc, hash) }
        assertThrows(IllegalArgumentException::class.java) { Anchor(AnchorId(1), "Desk", AnchorType.Nfc, "abc") }
        assertThrows(IllegalArgumentException::class.java) { Place(PlaceId(1), "", 0.0, 0.0, 100f) }
        assertThrows(IllegalArgumentException::class.java) { Place(PlaceId(1), "Gym", 91.0, 0.0, 100f) }
        assertThrows(IllegalArgumentException::class.java) { Place(PlaceId(1), "Gym", 0.0, 181.0, 100f) }
        assertThrows(IllegalArgumentException::class.java) { Place(PlaceId(1), "Gym", 0.0, 0.0, 0f) }
        assertThrows(IllegalArgumentException::class.java) { Space(SpaceId(1), "", "AA", -60) }
        assertThrows(IllegalArgumentException::class.java) { Space(SpaceId(1), "Bed", "AA", 5) }
        assertThrows(IllegalArgumentException::class.java) { Space(SpaceId(1), "Bed", "AA", -60, hysteresisDb = -1) }
        assertThrows(IllegalArgumentException::class.java) { Space(SpaceId(1), "Bed", "AA", -60, dwellSeconds = -1) }

        Anchor(AnchorId(1), "Desk", AnchorType.Nfc, hash, modeId = ModeId(2))
        Place(PlaceId(1), "Gym", 19.07, 72.87, 150f)
        Space(SpaceId(1), "Bedroom", "AA:BB:CC:DD:EE:FF", -65)
    }

    @Test
    fun `types round-trip through JSON with instants as epoch millis`() {
        val mode = Mode(
            id = ModeId(3),
            name = "Sleep",
            appPolicy = AppPolicy.AllowList(setOf("com.alarm")),
            quietActions = QuietActions(doNotDisturb = true, ringer = RingerMode.Silent),
            startTriggers = setOf(StartTrigger.Schedule(setOf(1), 1320, 420), StartTrigger.UsbConnected),
            exitRule = ExitRule.TimerEnds(480),
        )
        val session = Session(SessionId(9), mode.id, mode.name, start, SessionSource.AnchorTap, mode.exitRule)
        val unlock = UnlockRecord(start, SessionId(9))

        assertThat(Json.decodeFromString<Mode>(Json.encodeToString(mode))).isEqualTo(mode)
        assertThat(Json.decodeFromString<Session>(Json.encodeToString(session))).isEqualTo(session)
        assertThat(Json.encodeToString(unlock)).contains("\"at\":${start.toEpochMilli()}")
        assertThat(Json.decodeFromString<UnlockRecord>(Json.encodeToString(unlock))).isEqualTo(unlock)
    }
}
