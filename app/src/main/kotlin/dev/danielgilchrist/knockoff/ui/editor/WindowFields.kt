package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.ui.shortName
import java.time.DayOfWeek

@Composable
internal fun WindowFields(
    window: WindowDraft,
    onWindowChange: (WindowDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DayOfWeek.entries.forEach { day ->
                FilterChip(
                    selected = day in window.days,
                    onClick = { onWindowChange(window.toggle(day)) },
                    label = { Text(text = day.shortName()) },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimeOfDayButton(
                label = "From",
                time = window.start,
                onTimeChange = { onWindowChange(window.copy(start = it)) },
            )
            TimeOfDayButton(
                label = "To",
                time = window.end,
                onTimeChange = { onWindowChange(window.copy(end = it)) },
            )
        }
        Text(text = window.spanDescription(), style = MaterialTheme.typography.bodySmall)
    }
}
