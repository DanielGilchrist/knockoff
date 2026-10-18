package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import dev.danielgilchrist.knockoff.ui.components.CheckboxListItem
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

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        confirmButton = { TextButton(onClick = onDismiss) { Text(text = "Done") } },
        title = { Text(text = "Apps") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(text = "Search") },
                    singleLine = true,
                )
                when (apps) {
                    InstalledApps.Loading -> {
                        Text(text = "Loading installed apps…")
                    }

                    is InstalledApps.Loaded -> {
                        LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                            items(apps.apps.filter { it.matches(query) }, key = { it.packageName.value }) { app ->
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
        },
    )
}

private fun InstalledApp.matches(query: String): Boolean =
    label.contains(query, ignoreCase = true) || packageName.value.contains(query, ignoreCase = true)
