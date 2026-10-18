package dev.danielgilchrist.knockoff.ui.preferences

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
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
import dev.danielgilchrist.knockoff.ui.components.ListScreen
import dev.danielgilchrist.knockoff.ui.components.ScreenNavigation
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

    ListScreen(title = "Settings", navigation = ScreenNavigation.Back(actions.onBack), modifier = modifier) {
        saveProblem(saveStatus)
        item { SectionHeader(text = "Setup") }
        item { SetupGroup(setup = setup, actions = actions) }
        item { SectionHeader(text = "Default time zone") }
        item { Caption(text = "Rules set to App default follow this.") }
        item {
            ListGroup(modifier = Modifier.selectableGroup()) {
                ZoneChoiceRows(
                    selection = ZoneSelection.Chosen(settings.rules.defaultZone),
                    onChoose = actions.onDefaultZoneChange,
                )
            }
        }
        item { SectionHeader(text = "Theme") }
        item {
            ChoiceSegments(
                options = Theme.entries,
                selected = settings.theme,
                label = { it.describe() },
                onSelect = actions.onThemeChange,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun SetupGroup(
    setup: Setup,
    actions: PreferencesActions,
    modifier: Modifier = Modifier,
) {
    val system = actions.system

    ListGroup(modifier = modifier) {
        SetupItem.of(setup).forEach { item ->
            SetupRow(
                item = item,
                onAction =
                    when (item.action) {
                        SetupAction.Access -> system.onOpenAccessSettings
                        SetupAction.Link -> system.onLinkDevice
                        SetupAction.ExactAlarms -> system.onAllowExactAlarms
                        SetupAction.Notifications -> system.onAllowNotifications
                    },
            )
        }
    }
}

private fun Theme.describe(): String =
    when (this) {
        Theme.System -> "System"
        Theme.Light -> "Light"
        Theme.Dark -> "Dark"
    }
