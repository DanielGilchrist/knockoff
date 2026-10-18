package dev.danielgilchrist.knockoff.ui.editor

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.ui.components.IconBadge

@Composable
internal fun ChoiceCard(
    @DrawableRes icon: Int,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colours = MaterialTheme.colorScheme

    OutlinedCard(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(
                    CardDefaults.outlinedShape,
                ).selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        colors =
            CardDefaults.outlinedCardColors(
                containerColor = if (selected) colours.primaryContainer else colours.surface,
                contentColor = if (selected) colours.onPrimaryContainer else colours.onSurface,
            ),
        border =
            BorderStroke(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colours.primary else colours.outlineVariant,
            ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconBadge(icon = icon)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = description, style = MaterialTheme.typography.bodyMedium)
            }
            RadioButton(selected = selected, onClick = null)
        }
    }
}
