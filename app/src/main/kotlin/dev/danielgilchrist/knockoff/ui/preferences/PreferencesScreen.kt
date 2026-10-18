package dev.danielgilchrist.knockoff.ui.preferences

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.domain.settings.Settings
import dev.danielgilchrist.knockoff.domain.settings.Theme
import dev.danielgilchrist.knockoff.ui.SaveStatus
import dev.danielgilchrist.knockoff.ui.Setup
import dev.danielgilchrist.knockoff.ui.components.Caption
import dev.danielgilchrist.knockoff.ui.components.ChoiceSegments
import dev.danielgilchrist.knockoff.ui.components.ListGroup
import dev.danielgilchrist.knockoff.ui.components.SectionHeader
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
        item { SectionHeader(text = "Setup") }
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
                SectionHeader(text = "Default time zone")
                Caption(text = "Rules set to App default follow this.")
                ListGroup(modifier = Modifier.padding(top = 8.dp).selectableGroup()) {
                    ZoneChoiceRows(
                        selection = ZoneSelection.Chosen(settings.rules.defaultZone),
                        onChoose = actions.onDefaultZoneChange,
                    )
                }
            }
        }
        item {
            Column {
                SectionHeader(text = "Theme")
                ChoiceSegments(
                    options = Theme.entries,
                    selected = settings.theme,
                    label = { it.describe() },
                    onSelect = actions.onThemeChange,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        item { OutlinedButton(onClick = actions.onBack) { Text(text = "Done") } }
    }
}

private fun Theme.describe(): String =
    when (this) {
        Theme.System -> "System"
        Theme.Light -> "Light"
        Theme.Dark -> "Dark"
    }
