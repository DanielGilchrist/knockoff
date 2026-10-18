package dev.danielgilchrist.knockoff.ui.components

import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

@Composable
internal fun CheckboxListItem(
    headline: String,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String = "",
) {
    ListItem(
        headlineContent = { Text(text = headline) },
        supportingContent = supportingText(supporting),
        leadingContent = { Checkbox(checked = checked, onCheckedChange = null) },
        colors = groupedItemColors(),
        modifier = modifier.toggleable(value = checked, onValueChange = { onToggle() }, role = Role.Checkbox),
    )
}
