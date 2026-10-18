package dev.danielgilchrist.knockoff.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.R
import dev.danielgilchrist.knockoff.ui.InstalledApps
import dev.danielgilchrist.knockoff.ui.NewChats
import dev.danielgilchrist.knockoff.ui.RuleChange
import dev.danielgilchrist.knockoff.ui.RuleOverview
import dev.danielgilchrist.knockoff.ui.SaveStatus
import dev.danielgilchrist.knockoff.ui.SettingsOrigin
import dev.danielgilchrist.knockoff.ui.SettingsState
import dev.danielgilchrist.knockoff.ui.Setup
import dev.danielgilchrist.knockoff.ui.SetupNotice
import dev.danielgilchrist.knockoff.ui.SystemActions
import dev.danielgilchrist.knockoff.ui.components.Caption
import dev.danielgilchrist.knockoff.ui.components.ListGroup
import dev.danielgilchrist.knockoff.ui.components.ListScreen
import dev.danielgilchrist.knockoff.ui.components.NoticeAction
import dev.danielgilchrist.knockoff.ui.components.NoticeCard
import dev.danielgilchrist.knockoff.ui.components.NoticeTone
import dev.danielgilchrist.knockoff.ui.components.ScreenFab
import dev.danielgilchrist.knockoff.ui.components.ScreenNavigation
import dev.danielgilchrist.knockoff.ui.components.SectionHeader
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
    ListScreen(
        title = "Knockoff",
        navigation = ScreenNavigation.Root,
        modifier = modifier,
        actions = {
            IconButton(onClick = actions.onOpenPreferences) {
                Icon(painter = painterResource(R.drawable.ic_settings), contentDescription = "Settings")
            }
        },
        fab = ScreenFab.Extended("New rule", R.drawable.ic_add, actions.onNewRule),
    ) {
        saveProblem(saveStatus)
        when (settings) {
            SettingsState.Loading -> {
                item { Caption(text = "Loading settings…") }
            }

            is SettingsState.Failed -> {
                item {
                    NoticeCard(
                        title = "Settings couldn't be loaded",
                        body = settings.detail,
                        tone = NoticeTone.Problem,
                        action = NoticeAction.Button("Replace with defaults", actions.onResetToDefaults),
                    )
                }
            }

            is SettingsState.Ready -> {
                setupBanner(SetupNotice.of(setup, settings.settings.rules), actions.system)
                newChatsCard(newChats, onOpen = actions.onOpenNewChats)
                rules(settings, apps, actions)
            }
        }
    }
}

private fun LazyListScope.setupBanner(
    notice: SetupNotice,
    system: SystemActions,
) {
    when (notice) {
        SetupNotice.Clear -> {}

        SetupNotice.NoAccess, SetupNotice.NoNotifications -> {
            item { SetupBanner(notice = notice, system = system) }
        }
    }
}

private fun LazyListScope.newChatsCard(
    newChats: NewChats,
    onOpen: () -> Unit,
) {
    when (newChats) {
        NewChats.Loading -> {}

        is NewChats.Failed -> {
            item {
                NoticeCard(
                    title = "New chats couldn't be loaded",
                    body = newChats.detail,
                    tone = NoticeTone.Problem,
                )
            }
        }

        is NewChats.Ready -> {
            val count = newChats.chats.size
            val (title, detail) =
                when (count) {
                    1 -> "1 new chat" to "Hold it with a rule or let it through"
                    else -> "$count new chats" to "Hold them with a rule or let them through"
                }

            if (count > 0) {
                item { NewChatsCard(title = title, detail = detail, onOpen = onOpen) }
            }
        }
    }
}

@Composable
private fun NewChatsCard(
    title: String,
    detail: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        val onContainer = MaterialTheme.colorScheme.onSecondaryContainer

        ListItem(
            headlineContent = { Text(text = title) },
            supportingContent = { Text(text = detail) },
            leadingContent = { Icon(painter = painterResource(R.drawable.ic_forum), contentDescription = null) },
            trailingContent = { Icon(painter = painterResource(R.drawable.ic_chevron), contentDescription = null) },
            colors =
                ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                    headlineColor = onContainer,
                    supportingColor = onContainer,
                    leadingIconColor = onContainer,
                    trailingIconColor = onContainer,
                ),
        )
    }
}

private fun LazyListScope.rules(
    settings: SettingsState.Ready,
    apps: InstalledApps,
    actions: HomeActions,
) {
    val rules = settings.settings.rules.rules

    item { SectionHeader(text = "Rules") }
    when (settings.origin) {
        SettingsOrigin.Saved -> {}

        SettingsOrigin.Defaults -> {
            item { Caption(text = "Using defaults. Nothing is saved until you change something.") }
        }
    }
    when {
        rules.isEmpty() -> {
            item { EmptyRules() }
        }

        else -> {
            item {
                ListGroup {
                    rules.forEach { rule ->
                        RuleRow(
                            rule = rule,
                            summary = RuleOverview.of(rule, apps::labelOf).sentence,
                            onOpen = { actions.onEditRule(rule) },
                            onStateChange = { state -> actions.onRuleChange(rule.name, RuleChange.SetState(state)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyRules(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_bedtime),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Text(text = "No rules yet", style = MaterialTheme.typography.titleMedium)
        Caption(text = "Add one to start holding notifications.")
    }
}
