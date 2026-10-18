package dev.danielgilchrist.knockoff.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.R
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.ui.InstalledApps
import dev.danielgilchrist.knockoff.ui.NewChats
import dev.danielgilchrist.knockoff.ui.NewChatsView
import dev.danielgilchrist.knockoff.ui.RuleChange
import dev.danielgilchrist.knockoff.ui.SaveStatus
import dev.danielgilchrist.knockoff.ui.components.Caption
import dev.danielgilchrist.knockoff.ui.components.ListScreen
import dev.danielgilchrist.knockoff.ui.components.NoticeCard
import dev.danielgilchrist.knockoff.ui.components.NoticeTone
import dev.danielgilchrist.knockoff.ui.components.ScreenNavigation
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

    ListScreen(title = "New chats", navigation = ScreenNavigation.Back(onBack), modifier = modifier) {
        item {
            Caption(
                text =
                    "These chats have notified you but aren't in a rule. Letting one through can't be undone yet.",
            )
        }
        saveProblem(saveStatus)
        when (val save = view.saveStatus) {
            SaveStatus.Idle -> {}

            is SaveStatus.Failed -> {
                item {
                    NoticeCard(
                        title = "Couldn't let the chat through",
                        body = save.detail,
                        tone = NoticeTone.Problem,
                    )
                }
            }
        }
        when (val chats = view.chats) {
            NewChats.Loading -> {}

            is NewChats.Failed -> {
                item {
                    NoticeCard(
                        title = "New chats couldn't be loaded",
                        body = chats.detail,
                        tone = NoticeTone.Problem,
                    )
                }
            }

            is NewChats.Ready -> {
                if (chats.chats.isEmpty()) {
                    item { AllCaughtUp() }
                }
                items(chats.chats, key = { "chat:${it.packageName.value}/${it.id.value}" }) { chat ->
                    NewChatCard(
                        chat = chat,
                        appLabel = apps.labelOf(chat.packageName),
                        rules = rules,
                        onAddTo = { name -> actions.onRuleChange(name, RuleChange.AddChat(chat)) },
                        onLetThrough = { actions.onLetChatThrough(chat) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AllCaughtUp(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_check_circle),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
        )
        Text(text = "All caught up", style = MaterialTheme.typography.titleMedium)
        Caption(text = "New chats show up here when they first notify you.")
    }
}
