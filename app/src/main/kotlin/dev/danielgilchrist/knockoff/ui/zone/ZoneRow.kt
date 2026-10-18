package dev.danielgilchrist.knockoff.ui.zone

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.danielgilchrist.knockoff.R
import dev.danielgilchrist.knockoff.ui.components.ListGroup
import dev.danielgilchrist.knockoff.ui.components.NavigationListItem
import java.time.ZoneId

@Composable
internal fun <T> ZoneRow(
    headline: String,
    label: (T) -> String,
    leading: List<LeadingZone<T>>,
    selected: T,
    fixed: (ZoneId) -> T,
    onPick: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by rememberSaveable { mutableStateOf(false) }

    ListGroup(modifier = modifier) {
        NavigationListItem(
            icon = R.drawable.ic_public,
            headline = headline,
            supporting = label(selected),
            onClick = { picking = true },
        )
    }

    if (picking) {
        ZonePicker(
            leading = leading,
            selected = selected,
            fixed = fixed,
            onPick = onPick,
            onDismiss = { picking = false },
        )
    }
}
