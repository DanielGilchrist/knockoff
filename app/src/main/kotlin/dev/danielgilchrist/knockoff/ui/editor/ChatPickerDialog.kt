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
import dev.danielgilchrist.knockoff.domain.notification.ConversationId
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.ChatPick
import dev.danielgilchrist.knockoff.domain.rule.Target
import dev.danielgilchrist.knockoff.ui.SeenChatsState
import dev.danielgilchrist.knockoff.ui.components.Caption
import dev.danielgilchrist.knockoff.ui.components.CheckboxListItem
import dev.danielgilchrist.knockoff.ui.components.PickerScreen
import dev.danielgilchrist.knockoff.ui.describe
import dev.danielgilchrist.knockoff.ui.toggled

@Composable
internal fun ChatPickerDialog(
    app: PackageName,
    seen: SeenChatsState,
    selected: Map<ConversationId, ChatPick>,
    onSelectedChange: (Map<ConversationId, ChatPick>) -> Unit,
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
            SeenChatsState.Loading, is SeenChatsState.Failed -> {
                item { Caption(text = seen.describe { "" }, modifier = Modifier.padding(16.dp)) }
            }

            is SeenChatsState.Loaded -> {
                chatRows(
                    chats = AppKind.seenChatsOf(app, seen),
                    query = query,
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
    selected: Map<ConversationId, ChatPick>,
    onSelectedChange: (Map<ConversationId, ChatPick>) -> Unit,
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
                    checked = chat.id in selected,
                    onToggle = { onSelectedChange(selected.toggled(chat.id, ChatPick.of(chat))) },
                )
            }
        }
    }
}
