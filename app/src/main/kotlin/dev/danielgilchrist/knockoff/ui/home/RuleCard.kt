package dev.danielgilchrist.knockoff.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleState
import dev.danielgilchrist.knockoff.ui.describe
import dev.danielgilchrist.knockoff.ui.describeTargets

@Composable
internal fun RuleCard(
    rule: Rule,
    onEdit: () -> Unit,
    onStateChange: (RuleState) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth().clickable(onClick = onEdit)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = rule.name.value, style = MaterialTheme.typography.titleMedium)
                Text(text = rule.policy.describe())
                rule.schedule.windows.forEach { Text(text = it.describe(), style = MaterialTheme.typography.bodySmall) }
                rule.schedule.breaks.forEach { Text(text = it.describe(), style = MaterialTheme.typography.bodySmall) }
                Text(text = rule.describeTargets(), style = MaterialTheme.typography.bodySmall)
            }
            RuleStateSwitch(state = rule.state, onStateChange = onStateChange)
        }
    }
}
