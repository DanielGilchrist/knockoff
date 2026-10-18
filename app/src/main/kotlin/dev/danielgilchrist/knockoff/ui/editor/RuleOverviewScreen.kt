package dev.danielgilchrist.knockoff.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.R
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleState
import dev.danielgilchrist.knockoff.ui.InstalledApps
import dev.danielgilchrist.knockoff.ui.RuleOverview
import dev.danielgilchrist.knockoff.ui.RuleSection
import dev.danielgilchrist.knockoff.ui.SaveStatus
import dev.danielgilchrist.knockoff.ui.components.ListGroup
import dev.danielgilchrist.knockoff.ui.components.ListScreen
import dev.danielgilchrist.knockoff.ui.components.NavigationListItem
import dev.danielgilchrist.knockoff.ui.components.ScreenNavigation
import dev.danielgilchrist.knockoff.ui.home.RuleStateSwitch
import dev.danielgilchrist.knockoff.ui.saveProblem

@Composable
internal fun RuleOverviewScreen(
    rule: Rule,
    apps: InstalledApps,
    saveStatus: SaveStatus,
    actions: RuleOverviewActions,
    modifier: Modifier = Modifier,
) {
    val overview = RuleOverview.of(rule, apps::labelOf)
    var deleting by rememberSaveable { mutableStateOf(false) }

    BackHandler(onBack = actions.onBack)

    ListScreen(
        title = rule.name.value,
        navigation = ScreenNavigation.Back(actions.onBack),
        modifier = modifier,
        actions = { TextButton(onClick = { actions.onEditSection(RuleSection.Name) }) { Text(text = "Rename") } },
    ) {
        item { SummaryCard(rule = rule, sentence = overview.sentence, onStateChange = actions.onStateChange) }
        saveProblem(saveStatus)
        item {
            ListGroup {
                NavigationListItem(
                    R.drawable.ic_apps,
                    "What",
                    overview.what,
                    onClick = { actions.onEditSection(RuleSection.What) },
                )
                NavigationListItem(R.drawable.ic_schedule, "When", overview.hours, onClick = {
                    actions.onEditSection(RuleSection.When)
                })
                NavigationListItem(
                    R.drawable.ic_tune,
                    "Advanced",
                    overview.advanced,
                    onClick = { actions.onEditSection(RuleSection.Advanced) },
                )
            }
        }
        item {
            TextButton(onClick = { deleting = true }) {
                Text(text = "Delete rule", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (deleting) {
        DeleteDialog(
            name = rule.name.value,
            onConfirm = {
                deleting = false
                actions.onDelete()
            },
            onDismiss = { deleting = false },
        )
    }
}

@Composable
private fun SummaryCard(
    rule: Rule,
    sentence: String,
    onStateChange: (RuleState) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = sentence,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f).padding(end = 12.dp),
            )
            RuleStateSwitch(
                state = rule.state,
                onStateChange = onStateChange,
                modifier = Modifier.semantics { contentDescription = rule.name.value },
            )
        }
    }
}

@Composable
private fun DeleteDialog(
    name: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Delete $name?") },
        text = { Text(text = "Its notifications stop being held. This can't be undone.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text(text = "Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(text = "Cancel") } },
    )
}
