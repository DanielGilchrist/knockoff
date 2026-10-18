package dev.danielgilchrist.knockoff.ui.preferences

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.domain.settings.Settings
import dev.danielgilchrist.knockoff.domain.settings.Theme
import dev.danielgilchrist.knockoff.ui.RadioRow
import dev.danielgilchrist.knockoff.ui.SaveStatus
import dev.danielgilchrist.knockoff.ui.Setup
import dev.danielgilchrist.knockoff.ui.saveProblem
import dev.danielgilchrist.knockoff.ui.zone.ZoneChoiceRows
import dev.danielgilchrist.knockoff.ui.zone.ZoneSelection

@Composable
internal fun PreferencesScreen(
    settings: Settings,
    setup: Setup,
    saveStatus: SaveStatus,
    actions: PreferencesActions,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = actions.onBack)

    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text(text = "Settings", style = MaterialTheme.typography.titleLarge) }
        saveProblem(saveStatus)
        item { Text(text = "Setup", style = MaterialTheme.typography.titleSmall) }
        item { AccessCard(access = setup.access, onOpenSettings = actions.system.onOpenAccessSettings) }
        item {
            SilentHoldsCard(
                link = setup.link,
                alarms = setup.alarms,
                onLinkDevice = actions.system.onLinkDevice,
                onAllowExactAlarms = actions.system.onAllowExactAlarms,
            )
        }
        item {
            NotificationsCard(
                permission = setup.notifications,
                onAllowNotifications = actions.system.onAllowNotifications,
            )
        }
        item {
            Column {
                Text(text = "Default time zone", style = MaterialTheme.typography.titleSmall)
                Text(text = "Rules set to \"App default\" follow this.", style = MaterialTheme.typography.bodySmall)
                ZoneChoiceRows(
                    selection = ZoneSelection.Chosen(settings.rules.defaultZone),
                    onChoose = actions.onDefaultZoneChange,
                )
            }
        }
        item {
            Column {
                Text(text = "Theme", style = MaterialTheme.typography.titleSmall)
                Theme.entries.forEach { theme ->
                    RadioRow(
                        text = theme.describe(),
                        selected = settings.theme == theme,
                        onClick = { actions.onThemeChange(theme) },
                    )
                }
            }
        }
        item { OutlinedButton(onClick = actions.onBack) { Text(text = "Done") } }
    }
}

private fun Theme.describe(): String =
    when (this) {
        Theme.System -> "Follow the system"
        Theme.Light -> "Light"
        Theme.Dark -> "Dark"
    }
