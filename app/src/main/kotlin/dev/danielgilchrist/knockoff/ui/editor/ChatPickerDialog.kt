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
import dev.danielgilchrist.knockoff.domain.rule.ChatKey
import dev.danielgilchrist.knockoff.domain.rule.Target
import dev.danielgilchrist.knockoff.ui.InstalledApps
import dev.danielgilchrist.knockoff.ui.SeenChatsState
import dev.danielgilchrist.knockoff.ui.components.CheckboxListItem
import dev.danielgilchrist.knockoff.ui.toggled

@Composable
internal fun ChatPickerDialog(
    seen: SeenChatsState,
    apps: InstalledApps,
    selected: Map<ChatKey, Target.Chat>,
    onSelectedChange: (Map<ChatKey, Target.Chat>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        confirmButton = { TextButton(onClick = onDismiss) { Text(text = "Done") } },
        title = { Text(text = "Chats") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(text = "Search") },
                    singleLine = true,
                )
                when (seen) {
                    SeenChatsState.Loading -> {
                        Text(text = "Loading chats…")
                    }

                    is SeenChatsState.Failed -> {
                        Text(text = "Couldn't read the chats seen so far: ${seen.detail}")
                    }

                    is SeenChatsState.Loaded -> {
                        ChatRows(
                            chats = seen.chats,
                            query = query,
                            apps = apps,
                            selected = selected,
                            onSelectedChange = onSelectedChange,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun ChatRows(
    chats: List<Target.Chat>,
    query: String,
    apps: InstalledApps,
    selected: Map<ChatKey, Target.Chat>,
    onSelectedChange: (Map<ChatKey, Target.Chat>) -> Unit,
) {
    val matching = chats.filter { it.name.value.contains(query, ignoreCase = true) }

    when {
        chats.isEmpty() -> {
            Text(text = "No chats yet. A chat shows up here once it has sent you a notification.")
        }

        matching.isEmpty() -> {
            Text(text = "No chats match \"$query\".")
        }

        else -> {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(matching, key = { "${it.packageName.value}/${it.id.value}" }) { chat ->
                    CheckboxListItem(
                        headline = chat.name.value,
                        checked = chat.key in selected,
                        onToggle = { onSelectedChange(selected.toggled(chat.key, chat)) },
                        supporting = apps.labelOf(chat.packageName),
                    )
                }
            }
        }
    }
}
