package dev.danielgilchrist.knockoff.ui.zone

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.ui.describe
import java.time.ZoneId

@Composable
internal fun ZonePickerDialog(
    onPick: (ZoneId) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val matching = remember(query) { zonesMatching(query) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(text = "Cancel") } },
        title = { Text(text = "Time zone") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(text = "Search, e.g. London") },
                    singleLine = true,
                )
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(matching, key = { it.id }) { zone ->
                        Text(
                            text = zone.describe(),
                            modifier = Modifier.fillMaxWidth().clickable { onPick(zone) }.padding(vertical = 12.dp),
                        )
                    }
                }
            }
        },
    )
}
