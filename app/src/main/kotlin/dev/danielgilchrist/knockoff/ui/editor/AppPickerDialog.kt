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
import dev.danielgilchrist.knockoff.ui.appicon.AppIconBadge
import dev.danielgilchrist.knockoff.ui.components.Caption
import dev.danielgilchrist.knockoff.ui.components.CheckboxListItem
import dev.danielgilchrist.knockoff.ui.components.PickerClose
import dev.danielgilchrist.knockoff.ui.components.PickerScreen
import dev.danielgilchrist.knockoff.ui.toggled

@Composable
internal fun AppPickerDialog(
    apps: InstalledApps,
    added: Set<PackageName>,
    selected: Set<PackageName>,
    onSelectedChange: (Set<PackageName>) -> Unit,
    onAdd: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }

    PickerScreen(
        title = "Add apps",
        query = query,
        onQueryChange = { query = it },
        searchHint = "Search apps",
        onDone = onAdd,
        done = appPickerDone(selected.size),
        close = PickerClose.Discard(onClose),
        modifier = modifier,
    ) {
        when (apps) {
            InstalledApps.Loading -> {
                item { Caption(text = "Loading installed apps…", modifier = Modifier.padding(16.dp)) }
            }

            is InstalledApps.Unavailable -> {
                item {
                    Caption(
                        text = "Android couldn't list your apps. Leave Knockoff and come back to try again.",
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            is InstalledApps.Loaded -> {
                val addable = apps.apps.filter { it.packageName !in added }
                val matching = addable.filter { it.matches(query) }

                when {
                    addable.isEmpty() -> {
                        item {
                            Caption(
                                text = "Every app is already in this rule.",
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }

                    matching.isEmpty() -> {
                        item { Caption(text = "No apps match \"$query\".", modifier = Modifier.padding(16.dp)) }
                    }

                    else -> {}
                }
                items(matching, key = { it.packageName.value }) { app ->
                    CheckboxListItem(
                        headline = app.label,
                        checked = app.packageName in selected,
                        onToggle = { onSelectedChange(selected.toggled(app.packageName)) },
                    ) {
                        AppIconBadge(app = app.packageName)
                    }
                }
            }
        }
    }
}

private fun InstalledApp.matches(query: String): Boolean =
    label.contains(query, ignoreCase = true) || packageName.value.contains(query, ignoreCase = true)
