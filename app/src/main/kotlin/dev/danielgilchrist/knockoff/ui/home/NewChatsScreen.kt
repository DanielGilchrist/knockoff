package dev.danielgilchrist.knockoff.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.ui.InstalledApps
import dev.danielgilchrist.knockoff.ui.NewChats
import dev.danielgilchrist.knockoff.ui.NewChatsView
import dev.danielgilchrist.knockoff.ui.RuleChange
import dev.danielgilchrist.knockoff.ui.SaveStatus
import dev.danielgilchrist.knockoff.ui.SettingsProblemCard
import dev.danielgilchrist.knockoff.ui.editor.ScreenHeader
import dev.danielgilchrist.knockoff.ui.saveProblem

@Composable
internal fun NewChatsScreen(
    view: NewChatsView,
    saveStatus: SaveStatus,
    apps: InstalledApps,
    rules: List<Rule>,
    actions: HomeActions,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    LazyColumn(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader(title = "New chats", onBack = onBack, modifier = Modifier.padding(8.dp)) }
        item {
            Text(
                text =
                    "Chats that have notified you but aren't in a rule yet. Hold them with a rule, or let them " +
                        "through to stop seeing them here. Letting one through can't be undone yet.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        saveProblem(saveStatus, Modifier.padding(horizontal = 16.dp))
        when (val save = view.saveStatus) {
            SaveStatus.Idle -> {}

            is SaveStatus.Failed -> {
                item {
                    SettingsProblemCard(
                        title = "Couldn't let the chat through",
                        detail = save.detail,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
        when (val chats = view.chats) {
            NewChats.Loading -> {}

            is NewChats.Failed -> {
                item {
                    SettingsProblemCard(
                        title = "New chats couldn't be loaded",
                        detail = chats.detail,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }

            is NewChats.Ready -> {
                if (chats.chats.isEmpty()) {
                    item { Text(text = "No new chats.", modifier = Modifier.padding(horizontal = 16.dp)) }
                }
                items(chats.chats, key = { "chat:${it.packageName.value}/${it.id.value}" }) { chat ->
                    NewChatCard(
                        chat = chat,
                        appLabel = apps.labelOf(chat.packageName),
                        rules = rules,
                        onAddTo = { name -> actions.onRuleChange(name, RuleChange.AddChat(chat)) },
                        onLetThrough = { actions.onLetChatThrough(chat) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}
