package dev.danielgilchrist.knockoff.ui.zone

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.R
import dev.danielgilchrist.knockoff.ui.components.PickerScreen
import dev.danielgilchrist.knockoff.ui.components.RadioListItem
import dev.danielgilchrist.knockoff.ui.components.SectionHeader
import dev.danielgilchrist.knockoff.ui.components.groupedItemColors
import dev.danielgilchrist.knockoff.ui.describe
import java.time.ZoneId

@Composable
internal fun <T> ZonePicker(
    leading: List<LeadingZone<T>>,
    selected: T,
    fixed: (ZoneId) -> T,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val matching = remember(query) { zonesMatching(query) }
    val pick = { value: T ->
        onPick(value)
        onDismiss()
    }

    PickerScreen(
        title = "Time zone",
        query = query,
        onQueryChange = { query = it },
        searchHint = "Search, e.g. London",
        onDone = onDismiss,
        modifier = modifier,
    ) {
        if (query.isBlank()) {
            item {
                Column(modifier = Modifier.selectableGroup()) {
                    leading.forEach { option ->
                        RadioListItem(
                            headline = option.label,
                            selected = option.value == selected,
                            onClick = { pick(option.value) },
                            supporting = option.explanation,
                        )
                    }
                }
            }
            item { SectionHeader(text = "All time zones", modifier = Modifier.padding(horizontal = 16.dp)) }
        }
        items(matching, key = { it.id }) { zone ->
            val chosen = fixed(zone) == selected

            ListItem(
                headlineContent = { Text(text = zone.describe()) },
                trailingContent = {
                    if (chosen) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                colors = groupedItemColors(),
                modifier = Modifier.selectable(selected = chosen, role = Role.RadioButton) { pick(fixed(zone)) },
            )
        }
    }
}
