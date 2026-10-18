package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.ui.InstalledApp
import dev.danielgilchrist.knockoff.ui.InstalledApps
import dev.danielgilchrist.knockoff.ui.components.Caption
import dev.danielgilchrist.knockoff.ui.components.CheckboxListItem
import dev.danielgilchrist.knockoff.ui.components.PickerScreen
import dev.danielgilchrist.knockoff.ui.toggled

@Composable
internal fun AppPickerDialog(
    apps: InstalledApps,
    selected: Set<PackageName>,
    onSelectedChange: (Set<PackageName>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }

    PickerScreen(
        title = "Choose apps",
        query = query,
        onQueryChange = { query = it },
        searchHint = "Search apps",
        onDone = onDismiss,
        modifier = modifier,
    ) {
        when (apps) {
            InstalledApps.Loading -> {
                item { Caption(text = "Loading installed apps…", modifier = Modifier.padding(16.dp)) }
            }

            is InstalledApps.Loaded -> {
                val matching = apps.apps.filter { it.matches(query) }

                if (matching.isEmpty()) {
                    item { Caption(text = "No apps match \"$query\".", modifier = Modifier.padding(16.dp)) }
                }
                items(matching, key = { it.packageName.value }) { app ->
                    CheckboxListItem(
                        headline = app.label,
                        checked = app.packageName in selected,
                        onToggle = { onSelectedChange(selected.toggled(app.packageName)) },
                    )
                }
            }
        }
    }
}

private fun InstalledApp.matches(query: String): Boolean =
    label.contains(query, ignoreCase = true) || packageName.value.contains(query, ignoreCase = true)
