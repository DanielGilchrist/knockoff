package dev.danielgilchrist.knockoff.ui.zone

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.danielgilchrist.knockoff.domain.zone.ZoneChoice
import dev.danielgilchrist.knockoff.ui.RadioRow
import dev.danielgilchrist.knockoff.ui.describe

private const val SPECIFIC = "A specific time zone"

@Composable
internal fun ZoneChoiceRows(
    selection: ZoneSelection,
    onChoose: (ZoneChoice) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    val (specific, specificSelected) =
        when (selection) {
            ZoneSelection.None -> {
                SPECIFIC to false
            }

            is ZoneSelection.Chosen -> {
                when (val choice = selection.choice) {
                    ZoneChoice.Device -> SPECIFIC to false
                    is ZoneChoice.Fixed -> choice.zone.describe() to true
                }
            }
        }

    Column(modifier = modifier) {
        RadioRow(
            text = "Device time zone",
            selected = selection == ZoneSelection.Chosen(ZoneChoice.Device),
            onClick = { onChoose(ZoneChoice.Device) },
        )
        RadioRow(text = specific, selected = specificSelected, onClick = { picking = true })
    }

    if (picking) {
        ZonePickerDialog(
            onPick = {
                onChoose(ZoneChoice.Fixed(it))
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}
