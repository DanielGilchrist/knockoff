package dev.danielgilchrist.knockoff.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.R
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleName
import dev.danielgilchrist.knockoff.domain.rule.RuleState
import dev.danielgilchrist.knockoff.domain.rule.Target
import dev.danielgilchrist.knockoff.ui.components.IconBadge
import dev.danielgilchrist.knockoff.ui.components.groupedItemColors
import dev.danielgilchrist.knockoff.ui.icon

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

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        ListItem(
            headlineContent = { Text(text = chat.name.value) },
            supportingContent = { Text(text = appLabel) },
            leadingContent = { Initial(name = chat.name.value) },
            colors = groupedItemColors(),
        )
        Row(
            modifier = Modifier.align(Alignment.End).padding(end = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onLetThrough) { Text(text = "Let through") }
            when (rules.size) {
                0 -> {}

                1 -> {
                    val only = rules.single()

                    FilledTonalButton(onClick = { onAddTo(only.name) }) {
                        Text(text = holdLabel(only), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                else -> {
                    FilledTonalButton(onClick = { choosingRule = true }) { Text(text = "Hold with…") }
                }
            }
        }
    }

    if (choosingRule) {
        RuleChooser(
            chatName = chat.name.value,
            rules = rules,
            onChoose = { name ->
                choosingRule = false
                onAddTo(name)
            },
            onDismiss = { choosingRule = false },
        )
    }
}

@Composable
private fun Initial(name: String) {
    Surface(modifier = Modifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
        Box(contentAlignment = Alignment.Center) {
            when (val initial = ChatInitial.of(name)) {
                ChatInitial.None -> {
                    Icon(
                        painter = painterResource(R.drawable.ic_forum),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                }

                is ChatInitial.Letter -> {
                    Text(text = initial.text, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun RuleChooser(
    chatName: String,
    rules: List<Rule>,
    onChoose: (RuleName) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(text = "Cancel") } },
        title = { Text(text = "Hold $chatName with…") },
        text = {
            Column {
                rules.forEach { rule ->
                    ListItem(
                        headlineContent = { Text(text = rule.name.value) },
                        supportingContent =
                            when (rule.state) {
                                RuleState.Enabled -> null
                                RuleState.Disabled -> ({ Text(text = "Off") })
                            },
                        leadingContent = {
                            IconBadge(
                                icon = rule.policy.icon,
                            )
                        },
                        colors = groupedItemColors(),
                        modifier = Modifier.clickable { onChoose(rule.name) },
                    )
                }
            }
        },
    )
}

private fun holdLabel(rule: Rule): String =
    when (rule.state) {
        RuleState.Enabled -> "Hold with ${rule.name.value}"
        RuleState.Disabled -> "Hold with ${rule.name.value} (off)"
    }
