package dev.danielgilchrist.knockoff.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimeTile(
    label: String,
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    note: TileNote = TileNote.None,
) {
    var picking by rememberSaveable { mutableStateOf(false) }

    Surface(
        onClick = { picking = true },
        modifier =
            modifier.semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription =
                    when (note) {
                        TileNote.None -> "$label $time"
                        is TileNote.Shown -> "$label $time ${note.text}"
                    }
            },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = time.toString(), style = MaterialTheme.typography.headlineMedium)
            when (note) {
                TileNote.None -> {}

                is TileNote.Shown -> {
                    Text(
                        text = note.text,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (picking) {
        val state = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = true)

        TimePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTimeChange(LocalTime.of(state.hour, state.minute))
                        picking = false
                    },
                ) { Text(text = "Set") }
            },
            title = { Text(text = "$label time") },
            dismissButton = { TextButton(onClick = { picking = false }) { Text(text = "Cancel") } },
        ) { TimePicker(state = state) }
    }
}
