package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.domain.zone.RuleZone
import dev.danielgilchrist.knockoff.ui.components.ListGroup
import dev.danielgilchrist.knockoff.ui.components.RadioListItem
import dev.danielgilchrist.knockoff.ui.components.SectionHeader
import dev.danielgilchrist.knockoff.ui.zone.ZoneChoiceRows
import dev.danielgilchrist.knockoff.ui.zone.ZoneSelection

@Composable
internal fun ZoneEditor(
    zone: RuleZone,
    onZoneChange: (RuleZone) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionHeader(text = "Time zone")
        ListGroup(modifier = Modifier.padding(top = 8.dp).selectableGroup()) {
            RadioListItem(
                headline = "App default",
                selected = zone == RuleZone.Default,
                onClick = { onZoneChange(RuleZone.Default) },
                supporting = "Follows the default in Settings",
            )
            ZoneChoiceRows(
                selection =
                    when (zone) {
                        RuleZone.Default -> ZoneSelection.None
                        is RuleZone.Chosen -> ZoneSelection.Chosen(zone.choice)
                    },
                onChoose = { onZoneChange(RuleZone.Chosen(it)) },
            )
        }
    }
}
