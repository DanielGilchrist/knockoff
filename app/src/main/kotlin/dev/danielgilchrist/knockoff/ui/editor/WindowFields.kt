package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.ui.components.DaysRow
import dev.danielgilchrist.knockoff.ui.components.TimeTile

@Composable
internal fun WindowFields(
    window: WindowDraft,
    onWindowChange: (WindowDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DaysRow(days = window.days, onToggle = { onWindowChange(window.toggle(it)) })
        Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TimeTile(
                label = "From",
                time = window.start,
                onTimeChange = { onWindowChange(window.copy(start = it)) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            TimeTile(
                label = "To",
                time = window.end,
                onTimeChange = { onWindowChange(window.copy(end = it)) },
                modifier = Modifier.weight(1f).fillMaxHeight(),
                note = window.endNote(),
            )
        }
    }
}
