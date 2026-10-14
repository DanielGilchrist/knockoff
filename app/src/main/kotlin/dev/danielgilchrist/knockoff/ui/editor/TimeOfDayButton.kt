package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimeOfDayButton(
    label: String,
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by rememberSaveable { mutableStateOf(false) }

    OutlinedButton(onClick = { picking = true }, modifier = modifier) { Text(text = "$label $time") }

    if (picking) {
        val state = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = true)

        AlertDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeChange(LocalTime.of(state.hour, state.minute))
                        picking = false
                    },
                ) { Text(text = "Set") }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(text = "Cancel") } },
            text = { TimePicker(state = state) },
        )
    }
}
