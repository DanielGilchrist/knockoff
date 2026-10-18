package dev.danielgilchrist.knockoff.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleState

@Composable
internal fun RuleRow(
    rule: Rule,
    summary: String,
    onOpen: () -> Unit,
    onStateChange: (RuleState) -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(text = rule.name.value) },
        supportingContent = { Text(text = summary) },
        trailingContent = {
            RuleStateSwitch(
                state = rule.state,
                onStateChange = onStateChange,
                modifier = Modifier.semantics { contentDescription = rule.name.value },
            )
        },
        modifier = modifier.clickable(onClick = onOpen),
    )
}
