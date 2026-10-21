package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.ChatPick
import dev.danielgilchrist.knockoff.domain.rule.NewChatsAsking
import dev.danielgilchrist.knockoff.domain.rule.Target
import dev.danielgilchrist.knockoff.ui.SeenChatsState
import dev.danielgilchrist.knockoff.ui.components.Caption
import dev.danielgilchrist.knockoff.ui.components.CheckboxListItem
import dev.danielgilchrist.knockoff.ui.components.LinkButton
import dev.danielgilchrist.knockoff.ui.components.SwitchListItem
import dev.danielgilchrist.knockoff.ui.describe
import dev.danielgilchrist.knockoff.ui.ruleCalled
import dev.danielgilchrist.knockoff.ui.toggled

private const val RECENT_CHATS = 5

@Composable
internal fun ChatRows(
    app: PackageName,
    label: String,
    ruleName: String,
    draft: AppDraft,
    seen: SeenChatsState,
    onDraftChange: (AppDraft) -> Unit,
    beforeAsking: @Composable () -> Unit,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    val seenHere = AppKind.seenChatsOf(app, seen)
    val shown = draft.chats.values.sortedBy { it.name.value } + recentUnpicked(seenHere, draft)

    Column {
        val note = seen.describe { "" }

        if (note.isNotEmpty()) Caption(text = note, modifier = Modifier.padding(horizontal = 16.dp))
        shown.forEach { chat ->
            CheckboxListItem(
                headline = chat.name.value,
                checked = chat.id in draft.chats,
                onToggle = { onDraftChange(draft.copy(chats = draft.chats.toggled(chat.id, chat))) },
            )
        }
        if (seenHere.size > shown.count { chat -> seenHere.any { it.id == chat.id } }) {
            LinkButton(
                text = "See all ${seenHere.size} chats",
                onClick = { picking = true },
                modifier = Modifier.padding(start = 16.dp),
            )
        }
        beforeAsking()
        SwitchListItem(
            headline = "Ask me about new chats",
            supporting =
                "Get a quiet notification when a new $label chat appears, so you can add it to " +
                    "${ruleCalled(ruleName)}.",
            checked = draft.asking == NewChatsAsking.Ask,
            onToggle = { onDraftChange(draft.copy(asking = toggledAsking(draft.asking))) },
        )
    }

    if (picking) {
        ChatPickerDialog(
            app = app,
            seen = seen,
            selected = draft.chats,
            onSelectedChange = { onDraftChange(draft.copy(chats = it)) },
            onDismiss = { picking = false },
        )
    }
}

private fun recentUnpicked(
    seen: List<Target.Chat>,
    draft: AppDraft,
): List<ChatPick> = seen.filter { it.id !in draft.chats }.take(RECENT_CHATS).map(ChatPick::of)

private fun toggledAsking(asking: NewChatsAsking): NewChatsAsking =
    when (asking) {
        NewChatsAsking.Ask -> NewChatsAsking.DontAsk
        NewChatsAsking.DontAsk -> NewChatsAsking.Ask
    }
