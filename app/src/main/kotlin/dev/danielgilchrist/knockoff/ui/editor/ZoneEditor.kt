package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.danielgilchrist.knockoff.domain.zone.RuleZone
import dev.danielgilchrist.knockoff.domain.zone.ZoneChoice
import dev.danielgilchrist.knockoff.ui.zone.RULE_ZONES
import dev.danielgilchrist.knockoff.ui.zone.ZoneRow
import dev.danielgilchrist.knockoff.ui.zone.label

@Composable
internal fun ZoneEditor(
    zone: RuleZone,
    onZoneChange: (RuleZone) -> Unit,
    modifier: Modifier = Modifier,
) {
    ZoneRow(
        headline = "Time zone",
        label = { it.label() },
        leading = RULE_ZONES,
        selected = zone,
        fixed = { RuleZone.Chosen(ZoneChoice.Fixed(it)) },
        onPick = onZoneChange,
        modifier = modifier,
    )
}
