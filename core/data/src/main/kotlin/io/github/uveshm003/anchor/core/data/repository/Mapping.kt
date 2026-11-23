package io.github.uveshm003.anchor.core.data.repository

import io.github.uveshm003.anchor.core.database.entity.AnchorEntity
import io.github.uveshm003.anchor.core.database.entity.ModeEntity
import io.github.uveshm003.anchor.core.database.entity.SessionEntity
import io.github.uveshm003.anchor.core.model.Anchor
import io.github.uveshm003.anchor.core.model.AnchorId
import io.github.uveshm003.anchor.core.model.Mode
import io.github.uveshm003.anchor.core.model.ModeId
import io.github.uveshm003.anchor.core.model.Session
import io.github.uveshm003.anchor.core.model.SessionId
import java.time.Instant

internal fun ModeEntity.asModel() = Mode(
    id = ModeId(id),
    name = name,
    icon = icon,
    color = color,
    priority = priority,
    appPolicy = appPolicy,
    quietActions = quietActions,
    startTriggers = startTriggers,
    exitRule = exitRule,
    strict = strict,
)

internal fun Mode.asEntity() = ModeEntity(
    id = id.value,
    name = name,
    icon = icon,
    color = color,
    priority = priority,
    appPolicy = appPolicy,
    quietActions = quietActions,
    startTriggers = startTriggers,
    exitRule = exitRule,
    strict = strict,
)

internal fun AnchorEntity.asModel() = Anchor(AnchorId(id), name, type, hash, modeId?.let(::ModeId))

internal fun Anchor.asEntity() = AnchorEntity(id.value, name, type, hash, modeId?.value)

internal fun SessionEntity.asModel() = Session(
    id = SessionId(id),
    modeId = ModeId(modeId),
    modeName = modeName,
    startedAt = Instant.ofEpochMilli(startedAt),
    startedBy = startedBy,
    exitRule = exitRule,
    endsAt = endsAt?.let(Instant::ofEpochMilli),
    endedAt = endedAt?.let(Instant::ofEpochMilli),
    endedBy = endedBy,
    blockAttempts = blockAttempts,
)

internal fun Session.asEntity() = SessionEntity(
    id = id.value,
    modeId = modeId.value,
    modeName = modeName,
    startedAt = startedAt.toEpochMilli(),
    startedBy = startedBy,
    exitRule = exitRule,
    endsAt = endsAt?.toEpochMilli(),
    endedAt = endedAt?.toEpochMilli(),
    endedBy = endedBy,
    blockAttempts = blockAttempts,
)
