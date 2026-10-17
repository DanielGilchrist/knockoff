package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.danielgilchrist.knockoff.ui.InstalledApps
import dev.danielgilchrist.knockoff.ui.SeenChatsState
import dev.danielgilchrist.knockoff.ui.toggled

@Composable
internal fun AskingEditor(
    apps: InstalledApps,
    seen: SeenChatsState,
    draft: RuleDraft,
    onDraftChange: (RuleDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    val askable = draft.askableApps(seen)

    if (askable.isNotEmpty()) {
        Column(modifier = modifier) {
            Text(text = "Ask about new chats", style = MaterialTheme.typography.titleSmall)
            Text(
                text =
                    "When a chat from one of these apps first notifies you, a quiet notification offers to add " +
                        "it to this rule or let it through.",
                style = MaterialTheme.typography.bodySmall,
            )
            askable.sortedBy { apps.labelOf(it) }.forEach { app ->
                CheckboxRow(
                    title = apps.labelOf(app),
                    subtitle = "Ask about its new chats",
                    checked = app in draft.asking,
                    onToggle = { onDraftChange(draft.copy(asking = draft.asking.toggled(app))) },
                )
            }
        }
    }
}
