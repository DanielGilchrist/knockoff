package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.ui.InstalledApps
import dev.danielgilchrist.knockoff.ui.SeenChatsState
import dev.danielgilchrist.knockoff.ui.components.Caption
import dev.danielgilchrist.knockoff.ui.components.ListGroup
import dev.danielgilchrist.knockoff.ui.components.SectionHeader
import dev.danielgilchrist.knockoff.ui.components.SwitchListItem
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
            SectionHeader(text = "Ask about new chats")
            Caption(text = "A quiet notification asks whether to hold each new chat with this rule.")
            ListGroup(modifier = Modifier.padding(top = 8.dp)) {
                askable.sortedBy { apps.labelOf(it) }.forEach { app ->
                    SwitchListItem(
                        headline = apps.labelOf(app),
                        checked = app in draft.asking,
                        onToggle = { onDraftChange(draft.copy(asking = draft.asking.toggled(app))) },
                    )
                }
            }
        }
    }
}
