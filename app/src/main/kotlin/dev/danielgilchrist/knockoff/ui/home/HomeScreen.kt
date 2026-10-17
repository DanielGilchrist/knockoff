package dev.danielgilchrist.knockoff.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.settings.Settings
import dev.danielgilchrist.knockoff.ui.InstalledApps
import dev.danielgilchrist.knockoff.ui.NewChats
import dev.danielgilchrist.knockoff.ui.NewChatsView
import dev.danielgilchrist.knockoff.ui.RuleChange
import dev.danielgilchrist.knockoff.ui.SaveStatus
import dev.danielgilchrist.knockoff.ui.SettingsOrigin
import dev.danielgilchrist.knockoff.ui.SettingsProblemCard
import dev.danielgilchrist.knockoff.ui.SettingsState
import dev.danielgilchrist.knockoff.ui.Setup
import dev.danielgilchrist.knockoff.ui.saveProblem

@Composable
internal fun HomeScreen(
    setup: Setup,
    settings: SettingsState,
    saveStatus: SaveStatus,
    newChats: NewChatsView,
    apps: InstalledApps,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { AccessCard(access = setup.access, onOpenSettings = actions.system.onOpenAccessSettings) }
        item {
            SilentHoldsCard(
                link = setup.link,
                alarms = setup.alarms,
                onLinkDevice = actions.system.onLinkDevice,
                onAllowExactAlarms = actions.system.onAllowExactAlarms,
            )
        }
        saveProblem(saveStatus)
        when (settings) {
            SettingsState.Loading -> {
                item { Text(text = "Loading settings…") }
            }

            is SettingsState.Failed -> {
                item {
                    SettingsProblemCard(
                        title = "Settings couldn't be loaded",
                        detail = settings.detail,
                    ) {
                        Button(onClick = actions.onResetToDefaults, modifier = Modifier.padding(top = 8.dp)) {
                            Text(text = "Replace with defaults")
                        }
                    }
                }
            }

            is SettingsState.Ready -> {
                when (settings.origin) {
                    SettingsOrigin.Saved -> {}

                    SettingsOrigin.Defaults -> {
                        item { Text(text = "Using defaults. Nothing is saved until you change something.") }
                    }
                }
                rules(settings.settings, actions)
                newChats(newChats, apps, settings.settings.rules.rules, actions)
            }
        }
    }
}

private fun LazyListScope.rules(
    settings: Settings,
    actions: HomeActions,
) {
    item { Text(text = "Rules", style = MaterialTheme.typography.titleLarge) }
    items(settings.rules.rules, key = { it.name.value }) { rule ->
        RuleCard(
            rule = rule,
            onEdit = { actions.onEditRule(rule) },
            onStateChange = { state -> actions.onRuleChange(rule.name, RuleChange.SetState(state)) },
        )
    }
    item {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = actions.onNewRule) { Text(text = "Add rule") }
            OutlinedButton(onClick = actions.onOpenPreferences) { Text(text = "Settings") }
        }
    }
}

private fun LazyListScope.newChats(
    view: NewChatsView,
    apps: InstalledApps,
    rules: List<Rule>,
    actions: HomeActions,
) {
    when (val chats = view.chats) {
        NewChats.Loading -> {}

        is NewChats.Failed -> {
            item { SettingsProblemCard(title = "New chats couldn't be loaded", detail = chats.detail) }
        }

        is NewChats.Ready -> {
            if (chats.chats.isNotEmpty()) {
                item { NewChatsHeader() }
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
    when (val save = view.saveStatus) {
        SaveStatus.Idle -> {}

        is SaveStatus.Failed -> {
            item { SettingsProblemCard(title = "Couldn't let the chat through", detail = save.detail) }
        }
    }
}

@Composable
private fun NewChatsHeader() {
    Column {
        Text(text = "New chats", style = MaterialTheme.typography.titleLarge)
        Text(
            text =
                "Chats that have notified you but aren't in a rule yet. Hold them with a rule, or let them " +
                    "through to stop seeing them here. Letting one through can't be undone yet.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
