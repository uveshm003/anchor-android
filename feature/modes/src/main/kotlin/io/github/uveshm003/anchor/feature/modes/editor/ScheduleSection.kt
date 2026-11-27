package io.github.uveshm003.anchor.feature.modes.editor

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.uveshm003.anchor.core.designsystem.component.AnchorIcons
import io.github.uveshm003.anchor.core.designsystem.component.AnchorListRow
import io.github.uveshm003.anchor.core.designsystem.component.ListRowTrailing
import io.github.uveshm003.anchor.core.designsystem.component.SectionHeader
import io.github.uveshm003.anchor.feature.modes.R
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle

/** "Start automatically": a weekly schedule (other triggers join as their adapters land). */
@Composable
internal fun ScheduleSection(state: ModeEditorUiState, onSchedule: (ScheduleDraft?) -> Unit) {
    val schedule = state.draft.schedule
    Column {
        SectionHeader(stringResource(R.string.feature_modes_start_automatically))
        AnchorListRow(
            title = stringResource(R.string.feature_modes_schedule),
            supportingText = stringResource(R.string.feature_modes_schedule_body),
            leadingIcon = AnchorIcons.Schedule,
            trailing = ListRowTrailing.Switch(schedule != null) { on -> onSchedule(if (on) ScheduleDraft() else null) },
        )
        if (schedule != null) ScheduleEditor(state, schedule, onSchedule)
    }
}

@Composable
private fun ScheduleEditor(state: ModeEditorUiState, schedule: ScheduleDraft, onSchedule: (ScheduleDraft?) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DayOfWeek.entries.forEach { day ->
                val selected = day.value in schedule.days
                FilterChip(
                    selected = selected,
                    onClick = {
                        val days = if (selected) schedule.days - day.value else schedule.days + day.value
                        onSchedule(schedule.copy(days = days))
                    },
                    label = { Text(day.getDisplayName(TextStyle.SHORT, locale)) },
                    modifier = Modifier.heightIn(min = 48.dp),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            TimeButton(R.string.feature_modes_schedule_from, schedule.startMinute) {
                onSchedule(schedule.copy(startMinute = it))
            }
            TimeButton(R.string.feature_modes_schedule_until, schedule.endMinute) {
                onSchedule(schedule.copy(endMinute = it))
            }
        }
        val next = state.nextScheduleStart
        val status = when {
            state.scheduleError || !schedule.valid -> stringResource(R.string.feature_modes_schedule_invalid)

            next != null -> stringResource(
                R.string.feature_modes_schedule_next,
                next.dayOfWeek.getDisplayName(TextStyle.FULL, locale),
                DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(next),
            )

            else -> null
        }
        status?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.scheduleError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        if (!state.exactAlarms) ExactAlarmNote()
    }
}

@Composable
private fun ExactAlarmNote() {
    val context = LocalContext.current
    Column {
        Text(stringResource(R.string.feature_modes_schedule_inexact), style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }) { Text(stringResource(R.string.feature_modes_schedule_allow_exact)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeButton(label: Int, minuteOfDay: Int, onChange: (Int) -> Unit) {
    var picking by rememberSaveable { mutableStateOf(false) }
    val time = LocalTime.of(minuteOfDay / MINUTES_PER_HOUR, minuteOfDay % MINUTES_PER_HOUR)
    val locale = LocalConfiguration.current.locales[0]
    val text = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(time)
    OutlinedButton(onClick = { picking = true }, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(stringResource(label, text))
    }
    if (picking) {
        val picker = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute)
        AlertDialog(
            onDismissRequest = { picking = false },
            text = { TimePicker(state = picker) },
            confirmButton = {
                TextButton(onClick = {
                    picking = false
                    onChange(picker.hour * MINUTES_PER_HOUR + picker.minute)
                }) { Text(stringResource(R.string.feature_modes_done)) }
            },
            dismissButton = {
                TextButton(onClick = { picking = false }) { Text(stringResource(R.string.feature_modes_cancel)) }
            },
        )
    }
}

private const val MINUTES_PER_HOUR = 60
