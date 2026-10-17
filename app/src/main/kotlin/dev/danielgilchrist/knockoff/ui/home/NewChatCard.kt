package dev.danielgilchrist.knockoff.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleName
import dev.danielgilchrist.knockoff.domain.rule.RuleState
import dev.danielgilchrist.knockoff.domain.rule.Target

@Composable
internal fun NewChatCard(
    chat: Target.Chat,
    appLabel: String,
    rules: List<Rule>,
    onAddTo: (RuleName) -> Unit,
    onLetThrough: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var choosingRule by rememberSaveable { mutableStateOf(false) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = chat.name.value, style = MaterialTheme.typography.titleMedium)
            Text(text = appLabel, style = MaterialTheme.typography.bodySmall)
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (rules.isNotEmpty()) {
                    Button(onClick = { choosingRule = true }) { Text(text = "Add to rule…") }
                }
                OutlinedButton(onClick = onLetThrough) { Text(text = "Let through") }
            }
        }
    }

    if (choosingRule) {
        AlertDialog(
            onDismissRequest = { choosingRule = false },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { choosingRule = false }) { Text(text = "Cancel") } },
            title = { Text(text = "Hold ${chat.name.value} with…") },
            text = {
                Column {
                    rules.forEach { rule ->
                        TextButton(
                            onClick = {
                                onAddTo(rule.name)
                                choosingRule = false
                            },
                        ) { Text(text = rule.label()) }
                    }
                }
            },
        )
    }
}

private fun Rule.label(): String =
    when (state) {
        RuleState.Enabled -> name.value
        RuleState.Disabled -> "${name.value} (off)"
    }
