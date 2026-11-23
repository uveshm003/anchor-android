package io.github.uveshm003.anchor.core.database

import androidx.room.TypeConverter
import io.github.uveshm003.anchor.core.model.AppPolicy
import io.github.uveshm003.anchor.core.model.ExitRule
import io.github.uveshm003.anchor.core.model.QuietActions
import io.github.uveshm003.anchor.core.model.StartTrigger
import kotlinx.serialization.json.Json

/** Structured rules as JSON columns. Enums use Room's built-in name mapping. */
internal class Converters {
    @TypeConverter fun fromAppPolicy(value: AppPolicy): String = json.encodeToString(value)

    @TypeConverter fun toAppPolicy(value: String): AppPolicy = json.decodeFromString(value)

    @TypeConverter fun fromExitRule(value: ExitRule): String = json.encodeToString(value)

    @TypeConverter fun toExitRule(value: String): ExitRule = json.decodeFromString(value)

    @TypeConverter fun fromQuietActions(value: QuietActions): String = json.encodeToString(value)

    @TypeConverter fun toQuietActions(value: String): QuietActions = json.decodeFromString(value)

    @TypeConverter fun fromStartTriggers(value: Set<StartTrigger>): String = json.encodeToString(value)

    @TypeConverter fun toStartTriggers(value: String): Set<StartTrigger> = json.decodeFromString(value)

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
