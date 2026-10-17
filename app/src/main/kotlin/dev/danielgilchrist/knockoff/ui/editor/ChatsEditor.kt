package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.danielgilchrist.knockoff.domain.rule.ChatKey
import dev.danielgilchrist.knockoff.domain.rule.Target
import dev.danielgilchrist.knockoff.ui.InstalledApps
import dev.danielgilchrist.knockoff.ui.SeenChatsState

@Composable
internal fun ChatsEditor(
    seen: SeenChatsState,
    apps: InstalledApps,
    selected: Map<ChatKey, Target.Chat>,
    onSelectedChange: (Map<ChatKey, Target.Chat>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(text = "Chats", style = MaterialTheme.typography.titleSmall)
        Text(
            text =
                "Hold single conversations in an app, such as work contacts on WhatsApp. A chat appears here " +
                    "once it has sent you a notification. You can add more later.",
            style = MaterialTheme.typography.bodySmall,
        )
        selected.forEach { (key, chat) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = chat.name.value)
                    Text(text = apps.labelOf(chat.packageName), style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = { onSelectedChange(selected - key) }) { Text(text = "Remove") }
            }
        }
        OutlinedButton(onClick = { picking = true }) { Text(text = "Choose chats") }
    }

    if (picking) {
        ChatPickerDialog(
            seen = seen,
            apps = apps,
            selected = selected,
            onSelectedChange = onSelectedChange,
            onDismiss = { picking = false },
        )
    }
}
