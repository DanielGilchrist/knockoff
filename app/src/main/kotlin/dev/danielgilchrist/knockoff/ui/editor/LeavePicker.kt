package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.danielgilchrist.knockoff.domain.dayoff.Leave
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LeavePicker(
    today: LocalDate,
    onPick: (Leave) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val todayMillis = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state =
        rememberDateRangePickerState(
            initialSelectedStartDateMillis = todayMillis,
            yearRange = today.year..today.year + 2,
            selectableDates =
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= todayMillis

                    override fun isSelectableYear(year: Int): Boolean = year >= today.year
                },
        )
    val choice =
        LeaveChoice.of(state.selectedStartDateMillis?.let(::utcDate), state.selectedEndDateMillis?.let(::utcDate))

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    when (choice) {
                        LeaveChoice.Unpicked -> {}

                        is LeaveChoice.Picked -> {
                            onPick(choice.leave)
                        }
                    }
                },
                enabled =
                    when (choice) {
                        LeaveChoice.Unpicked -> false
                        is LeaveChoice.Picked -> true
                    },
            ) { Text(text = "Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(text = "Cancel") } },
        modifier = modifier,
    ) {
        DateRangePicker(
            state = state,
            showModeToggle = false,
        )
    }
}

private fun utcDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
