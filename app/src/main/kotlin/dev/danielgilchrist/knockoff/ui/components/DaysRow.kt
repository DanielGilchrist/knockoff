package dev.danielgilchrist.knockoff.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun DaysRow(
    days: Set<DayOfWeek>,
    onToggle: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier,
) {
    MultiChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        DayOfWeek.entries.forEachIndexed { index, day ->
            SegmentedButton(
                checked = day in days,
                onCheckedChange = { onToggle(day) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = DayOfWeek.entries.size),
                icon = {},
                label = {
                    Text(
                        text = day.getDisplayName(TextStyle.NARROW, Locale.UK),
                        fontWeight = if (day in days) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                modifier = Modifier.semantics { contentDescription = day.getDisplayName(TextStyle.FULL, Locale.UK) },
            )
        }
    }
}
