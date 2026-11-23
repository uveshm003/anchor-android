package io.github.uveshm003.anchor.core.model

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class ModeId(val value: Long)

@Serializable
@JvmInline
value class AnchorId(val value: Long)

@Serializable
@JvmInline
value class SessionId(val value: Long)

@Serializable
@JvmInline
value class PlaceId(val value: Long)

@Serializable
@JvmInline
value class SpaceId(val value: Long)
