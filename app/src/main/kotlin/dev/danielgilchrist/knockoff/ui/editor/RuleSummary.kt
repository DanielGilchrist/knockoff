package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.ui.describe
import dev.danielgilchrist.knockoff.ui.describeTargets

@Composable
internal fun RuleSummary(
    rule: Rule,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(text = rule.policy.describe())
        rule.schedule.windows.forEach { Text(text = it.describe(), style = MaterialTheme.typography.bodySmall) }
        rule.schedule.breaks.forEach { Text(text = it.describe(), style = MaterialTheme.typography.bodySmall) }
        Text(text = rule.describeTargets(), style = MaterialTheme.typography.bodySmall)
    }
}
