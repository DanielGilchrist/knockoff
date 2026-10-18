package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
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
import dev.danielgilchrist.knockoff.ui.components.Caption
import dev.danielgilchrist.knockoff.ui.components.CheckboxListItem
import dev.danielgilchrist.knockoff.ui.components.PickerScreen
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

    PickerScreen(
        title = "Choose chats",
        query = query,
        onQueryChange = { query = it },
        searchHint = "Search chats",
        onDone = onDismiss,
        modifier = modifier,
    ) {
        when (seen) {
            SeenChatsState.Loading -> {
                item { Caption(text = "Loading chats…", modifier = Modifier.padding(16.dp)) }
            }

            is SeenChatsState.Failed -> {
                item {
                    Caption(
                        text = "Couldn't read the chats seen so far: ${seen.detail}",
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            is SeenChatsState.Loaded -> {
                chatRows(
                    chats = seen.chats,
                    query = query,
                    apps = apps,
                    selected = selected,
                    onSelectedChange = onSelectedChange,
                )
            }
        }
    }
}

private fun LazyListScope.chatRows(
    chats: List<Target.Chat>,
    query: String,
    apps: InstalledApps,
    selected: Map<ChatKey, Target.Chat>,
    onSelectedChange: (Map<ChatKey, Target.Chat>) -> Unit,
) {
    val matching = chats.filter { it.name.value.contains(query, ignoreCase = true) }

    when {
        chats.isEmpty() -> {
            item {
                Caption(
                    text = "No chats yet. A chat shows up once it has notified you.",
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        matching.isEmpty() -> {
            item { Caption(text = "No chats match \"$query\".", modifier = Modifier.padding(16.dp)) }
        }

        else -> {
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
