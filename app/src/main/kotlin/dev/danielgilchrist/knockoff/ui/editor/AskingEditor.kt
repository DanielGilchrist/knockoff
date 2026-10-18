package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.ui.InstalledApps
import dev.danielgilchrist.knockoff.ui.SeenChatsState
import dev.danielgilchrist.knockoff.ui.components.Caption
import dev.danielgilchrist.knockoff.ui.components.InlineSwitch
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
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            HorizontalDivider()
            Text(text = "Ask about new chats", style = MaterialTheme.typography.titleSmall)
            Caption(text = "A quiet notification asks whether to hold each new chat with this rule.")
            askable.sortedBy { apps.labelOf(it) }.forEach { app ->
                InlineSwitch(
                    text = apps.labelOf(app),
                    checked = app in draft.asking,
                    onToggle = { onDraftChange(draft.copy(asking = draft.asking.toggled(app))) },
                )
            }
        }
    }
}
