package dev.danielgilchrist.knockoff.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.domain.settings.Settings
import dev.danielgilchrist.knockoff.ui.SaveStatus
import dev.danielgilchrist.knockoff.ui.SettingsOrigin
import dev.danielgilchrist.knockoff.ui.SettingsState
import dev.danielgilchrist.knockoff.ui.Setup

@Composable
internal fun HomeScreen(
    setup: Setup,
    settings: SettingsState,
    saveStatus: SaveStatus,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { AccessCard(access = setup.access, onOpenSettings = actions.onOpenAccessSettings) }
        item {
            SilentHoldsCard(
                link = setup.link,
                alarms = setup.alarms,
                onLinkDevice = actions.onLinkDevice,
                onAllowExactAlarms = actions.onAllowExactAlarms,
            )
        }
        when (saveStatus) {
            SaveStatus.Idle -> {}

            is SaveStatus.Failed -> {
                item {
                    SettingsProblemCard(
                        title = "Couldn't save settings",
                        detail = saveStatus.detail,
                    )
                }
            }
        }
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
            onStateChange = { state -> actions.onRuleStateChange(rule.name, state) },
        )
    }
    item { Button(onClick = actions.onNewRule) { Text(text = "Add rule") } }
}
