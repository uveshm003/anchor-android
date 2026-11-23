package io.github.uveshm003.anchor.core.engine

import io.github.uveshm003.anchor.core.model.Anchor
import io.github.uveshm003.anchor.core.model.AnchorId
import io.github.uveshm003.anchor.core.model.AnchorType
import io.github.uveshm003.anchor.core.model.AppPolicy
import io.github.uveshm003.anchor.core.model.ExitRule
import io.github.uveshm003.anchor.core.model.Mode
import io.github.uveshm003.anchor.core.model.ModeId
import io.github.uveshm003.anchor.core.model.Session
import io.github.uveshm003.anchor.core.model.SessionId
import io.github.uveshm003.anchor.core.model.SessionSource
import io.github.uveshm003.anchor.core.model.StartTrigger
import java.time.Instant

internal object Fixtures {
    val now: Instant = Instant.parse("2026-11-23T10:00:00Z")

    const val DIALER = "com.android.dialer"
    const val LAUNCHER = "com.android.launcher"
    const val SOCIAL = "com.social"

    val desk = Anchor(AnchorId(1), "Desk", AnchorType.Nfc, "a".repeat(64))
    val bedside = Anchor(AnchorId(2), "Bedside", AnchorType.Nfc, "b".repeat(64), modeId = ModeId(2))

    val work = Mode(ModeId(1), "Work", appPolicy = AppPolicy.BlockList(setOf(SOCIAL, DIALER)))
    val timer = Mode(ModeId(2), "Nap", exitRule = ExitRule.TimerEnds(30))
    val specific = Mode(ModeId(3), "Desk only", exitRule = ExitRule.RequiresAnchor(desk.id))
    val trigger = Mode(ModeId(4), "Commute", exitRule = ExitRule.TriggerEnds)
    val anchorOnly = Mode(ModeId(5), "Anchor only", startTriggers = setOf(StartTrigger.AnchorTap))
    val holdOnly = Mode(ModeId(6), "Hold only", startTriggers = setOf(StartTrigger.Hold))
    val allow = Mode(ModeId(7), "Allow", appPolicy = AppPolicy.AllowList(setOf("com.maps")))

    val env = Environment(
        now = now,
        modes = listOf(work, timer, specific, trigger, anchorOnly, holdOnly, allow),
        anchors = listOf(desk, bedside),
        defaultModeId = work.id,
        essentials = setOf(DIALER, LAUNCHER),
    )

    /** A session of [mode] started at [now] by holding. */
    fun activeIn(mode: Mode, state: EngineState = EngineState()): EngineState {
        val endsAt = (mode.exitRule as? ExitRule.TimerEnds)?.let { now.plusSeconds(it.minutes * SECONDS_PER_MINUTE) }
        val session = Session(
            id = SessionId(state.nextSessionId),
            modeId = mode.id,
            modeName = mode.name,
            startedAt = now,
            startedBy = SessionSource.Hold,
            exitRule = mode.exitRule,
            endsAt = endsAt,
        )
        return state.copy(active = ActiveSession(session, mode), nextSessionId = state.nextSessionId + 1)
    }

    private const val SECONDS_PER_MINUTE = 60L
}
