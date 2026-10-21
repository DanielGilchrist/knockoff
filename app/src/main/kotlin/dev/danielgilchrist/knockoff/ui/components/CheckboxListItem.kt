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
    CheckboxRow(
        headline = headline,
        supporting = supporting,
        checked = checked,
        onToggle = onToggle,
        modifier = modifier,
        leading = { Checkbox(checked = checked, onCheckedChange = null) },
        trailing = null,
    )
}

@Composable
internal fun CheckboxListItem(
    headline: String,
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String = "",
    leading: @Composable () -> Unit,
) {
    CheckboxRow(
        headline = headline,
        supporting = supporting,
        checked = checked,
        onToggle = onToggle,
        modifier = modifier,
        leading = leading,
        trailing = { Checkbox(checked = checked, onCheckedChange = null) },
    )
}

@Composable
private fun CheckboxRow(
    headline: String,
    supporting: String,
    checked: Boolean,
    onToggle: () -> Unit,
    leading: @Composable () -> Unit,
    trailing: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(text = headline) },
        supportingContent = supportingText(supporting),
        leadingContent = leading,
        trailingContent = trailing,
        colors = groupedItemColors(),
        modifier = modifier.toggleable(value = checked, onValueChange = { onToggle() }, role = Role.Checkbox),
    )
}
