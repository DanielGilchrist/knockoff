package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.danielgilchrist.knockoff.domain.zone.RuleZone
import dev.danielgilchrist.knockoff.domain.zone.ZoneChoice
import dev.danielgilchrist.knockoff.ui.RadioRow
import dev.danielgilchrist.knockoff.ui.describe
import dev.danielgilchrist.knockoff.ui.zone.ZonePickerDialog

private const val SPECIFIC = "A specific time zone"

@Composable
internal fun ZoneEditor(
    zone: RuleZone,
    onZoneChange: (RuleZone) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    val (selection, specific) =
        when (zone) {
            RuleZone.Default -> {
                ZoneSelection.AppDefault to SPECIFIC
            }

            is RuleZone.Chosen -> {
                when (val choice = zone.choice) {
                    ZoneChoice.Device -> ZoneSelection.Device to SPECIFIC
                    is ZoneChoice.Fixed -> ZoneSelection.Specific to choice.zone.describe()
                }
            }
        }

    Column(modifier = modifier) {
        Text(text = "Time zone", style = MaterialTheme.typography.titleSmall)
        RadioRow(
            text = "App default",
            selected = selection == ZoneSelection.AppDefault,
            onClick = { onZoneChange(RuleZone.Default) },
        )
        RadioRow(
            text = "Device time zone",
            selected = selection == ZoneSelection.Device,
            onClick = { onZoneChange(RuleZone.Chosen(ZoneChoice.Device)) },
        )
        RadioRow(text = specific, selected = selection == ZoneSelection.Specific, onClick = { picking = true })
    }

    if (picking) {
        ZonePickerDialog(
            onPick = {
                onZoneChange(RuleZone.Chosen(ZoneChoice.Fixed(it)))
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}
