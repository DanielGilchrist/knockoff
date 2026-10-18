package dev.danielgilchrist.knockoff.ui.components

import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

@Composable
internal fun RadioListItem(
    headline: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String = "",
) {
    ListItem(
        headlineContent = { Text(text = headline) },
        supportingContent = supportingText(supporting),
        leadingContent = { RadioButton(selected = selected, onClick = null) },
        colors = groupedItemColors(),
        modifier = modifier.selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
    )
}
