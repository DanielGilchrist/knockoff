package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.danielgilchrist.knockoff.domain.zone.RuleZone
import dev.danielgilchrist.knockoff.ui.RadioRow
import dev.danielgilchrist.knockoff.ui.zone.ZoneChoiceRows
import dev.danielgilchrist.knockoff.ui.zone.ZoneSelection

@Composable
internal fun ZoneEditor(
    zone: RuleZone,
    onZoneChange: (RuleZone) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(text = "Time zone", style = MaterialTheme.typography.titleSmall)
        RadioRow(
            text = "App default",
            selected = zone == RuleZone.Default,
            onClick = { onZoneChange(RuleZone.Default) },
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
