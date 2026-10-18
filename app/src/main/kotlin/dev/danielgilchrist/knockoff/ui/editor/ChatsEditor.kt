package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.R
import dev.danielgilchrist.knockoff.ui.InstalledApps
import dev.danielgilchrist.knockoff.ui.SeenChatsState
import dev.danielgilchrist.knockoff.ui.components.AddButton
import dev.danielgilchrist.knockoff.ui.components.Caption
import dev.danielgilchrist.knockoff.ui.components.CategoryCard
import dev.danielgilchrist.knockoff.ui.components.RemovableChip

@Composable
internal fun ChatsEditor(
    seen: SeenChatsState,
    apps: InstalledApps,
    draft: RuleDraft,
    onDraftChange: (RuleDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    val selected = draft.chats

    CategoryCard(icon = R.drawable.ic_forum, title = "Chats", count = selected.size, modifier = modifier) {
        Caption(
            text = "Single conversations, such as work contacts on WhatsApp. A chat shows up once it has notified you.",
        )
        if (selected.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                selected.entries.sortedBy { it.value.name.value }.forEach { (key, chat) ->
                    RemovableChip(
                        label = "${chat.name.value} · ${apps.labelOf(chat.packageName)}",
                        onRemove = { onDraftChange(draft.copy(chats = selected - key)) },
                    )
                }
            }
        }
        AddButton(text = "Choose chats", onClick = { picking = true })
        AskingEditor(apps = apps, seen = seen, draft = draft, onDraftChange = onDraftChange)
    }

    if (picking) {
        ChatPickerDialog(
            seen = seen,
            apps = apps,
            selected = selected,
            onSelectedChange = { onDraftChange(draft.copy(chats = it)) },
            onDismiss = { picking = false },
        )
    }
}
