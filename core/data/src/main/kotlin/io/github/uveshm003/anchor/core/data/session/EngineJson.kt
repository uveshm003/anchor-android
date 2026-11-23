package io.github.uveshm003.anchor.core.data.session

import io.github.uveshm003.anchor.core.engine.Command
import io.github.uveshm003.anchor.core.engine.EngineState
import kotlinx.serialization.json.Json

/** How the engine state and outbox commands are stored. Unknown keys are ignored so old rows stay readable. */
internal object EngineJson {
    private val json = Json { ignoreUnknownKeys = true }

    fun encodeState(state: EngineState): String = json.encodeToString(state)

    fun decodeState(text: String): EngineState = json.decodeFromString(text)

    fun encodeCommand(command: Command): String = json.encodeToString(command)

    fun decodeCommand(text: String): Command = json.decodeFromString(text)
}
