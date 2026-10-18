package dev.danielgilchrist.knockoff.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.R
import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleState
import dev.danielgilchrist.knockoff.ui.components.IconBadge
import dev.danielgilchrist.knockoff.ui.components.groupedItemColors

@Composable
internal fun RuleRow(
    rule: Rule,
    summary: String,
    onOpen: () -> Unit,
    onStateChange: (RuleState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val icon =
        when (rule.policy) {
            Policy.DeliverOnlyDuring -> R.drawable.ic_work
            Policy.HoldDuring -> R.drawable.ic_bedtime
        }

    ListItem(
        headlineContent = { Text(text = rule.name.value) },
        supportingContent = { Text(text = summary, maxLines = 3, overflow = TextOverflow.Ellipsis) },
        leadingContent = {
            when (rule.state) {
                RuleState.Enabled -> {
                    IconBadge(
                        icon = icon,
                        container = MaterialTheme.colorScheme.primaryContainer,
                        content = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }

                RuleState.Disabled -> {
                    IconBadge(
                        icon = icon,
                        container = MaterialTheme.colorScheme.surfaceContainerHighest,
                        content = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                VerticalDivider(modifier = Modifier.height(32.dp).padding(end = 16.dp))
                RuleStateSwitch(
                    state = rule.state,
                    onStateChange = onStateChange,
                    modifier = Modifier.semantics { contentDescription = rule.name.value },
                )
            }
        },
        colors = groupedItemColors(),
        modifier = modifier.clickable(onClick = onOpen),
    )
}
