package dev.danielgilchrist.knockoff.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.ui.InstalledApps
import dev.danielgilchrist.knockoff.ui.NewChats
import dev.danielgilchrist.knockoff.ui.RuleChange
import dev.danielgilchrist.knockoff.ui.RuleOverview
import dev.danielgilchrist.knockoff.ui.SaveStatus
import dev.danielgilchrist.knockoff.ui.SectionRow
import dev.danielgilchrist.knockoff.ui.SettingsOrigin
import dev.danielgilchrist.knockoff.ui.SettingsProblemCard
import dev.danielgilchrist.knockoff.ui.SettingsState
import dev.danielgilchrist.knockoff.ui.Setup
import dev.danielgilchrist.knockoff.ui.SetupNotice
import dev.danielgilchrist.knockoff.ui.saveProblem

@Composable
internal fun HomeScreen(
    setup: Setup,
    settings: SettingsState,
    saveStatus: SaveStatus,
    newChats: NewChats,
    apps: InstalledApps,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Header(onOpenPreferences = actions.onOpenPreferences) }
        saveProblem(saveStatus, Modifier.padding(horizontal = 16.dp))
        when (settings) {
            SettingsState.Loading -> {
                item { Text(text = "Loading settings…", modifier = Modifier.padding(horizontal = 16.dp)) }
            }

            is SettingsState.Failed -> {
                item {
                    SettingsProblemCard(
                        title = "Settings couldn't be loaded",
                        detail = settings.detail,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        Button(onClick = actions.onResetToDefaults, modifier = Modifier.padding(top = 8.dp)) {
                            Text(text = "Replace with defaults")
                        }
                    }
                }
            }

            is SettingsState.Ready -> {
                item {
                    SetupBanner(
                        notice = SetupNotice.of(setup, settings.settings.rules),
                        system = actions.system,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                newChatsRow(newChats, onOpen = actions.onOpenNewChats)
                rules(settings, apps, actions)
            }
        }
    }
}

@Composable
private fun Header(onOpenPreferences: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "Knockoff", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        TextButton(onClick = onOpenPreferences) { Text(text = "Settings") }
    }
}

private fun LazyListScope.newChatsRow(
    newChats: NewChats,
    onOpen: () -> Unit,
) {
    when (newChats) {
        NewChats.Loading -> {}

        is NewChats.Failed -> {
            item { SectionRow(title = "New chats couldn't be loaded", value = "Open to see why", onClick = onOpen) }
        }

        is NewChats.Ready -> {
            val count = newChats.chats.size

            if (count > 0) {
                item {
                    SectionRow(
                        title = if (count == 1) "1 new chat" else "$count new chats",
                        value = "Hold them with a rule, or let them through",
                        onClick = onOpen,
                    )
                }
            }
        }
    }
}

private fun LazyListScope.rules(
    settings: SettingsState.Ready,
    apps: InstalledApps,
    actions: HomeActions,
) {
    val rules = settings.settings.rules.rules

    item {
        Text(
            text = "Rules",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
        )
    }
    when (settings.origin) {
        SettingsOrigin.Saved -> {}

        SettingsOrigin.Defaults -> {
            item {
                Text(
                    text = "Using defaults. Nothing is saved until you change something.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
    if (rules.isEmpty()) {
        item {
            Text(
                text = "No rules yet. Add one to start holding notifications.",
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
    items(rules, key = { it.name.value }) { rule ->
        RuleRow(
            rule = rule,
            summary = RuleOverview.of(rule, apps::labelOf).sentence,
            onOpen = { actions.onEditRule(rule) },
            onStateChange = { state -> actions.onRuleChange(rule.name, RuleChange.SetState(state)) },
        )
    }
    item {
        Button(onClick = actions.onNewRule, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(text = "Add rule")
        }
    }
}
