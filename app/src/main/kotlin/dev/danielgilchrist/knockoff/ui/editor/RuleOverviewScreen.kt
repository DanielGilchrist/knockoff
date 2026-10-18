package dev.danielgilchrist.knockoff.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.ui.InstalledApps
import dev.danielgilchrist.knockoff.ui.RuleOverview
import dev.danielgilchrist.knockoff.ui.RuleSection
import dev.danielgilchrist.knockoff.ui.SaveStatus
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

    LazyColumn(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item {
            ScreenHeader(title = rule.name.value, onBack = actions.onBack, modifier = Modifier.padding(8.dp)) {
                TextButton(onClick = { actions.onEditSection(RuleSection.Name) }) { Text(text = "Rename") }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = overview.sentence,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                    )
                    RuleStateSwitch(
                        state = rule.state,
                        onStateChange = actions.onStateChange,
                        modifier = Modifier.semantics { contentDescription = rule.name.value },
                    )
                }
            }
        }
        saveProblem(saveStatus)
        item { SectionRow("What", overview.what, onClick = { actions.onEditSection(RuleSection.What) }) }
        item { SectionRow("When", overview.hours, onClick = { actions.onEditSection(RuleSection.When) }) }
        item { SectionRow("Advanced", overview.advanced, onClick = { actions.onEditSection(RuleSection.Advanced) }) }
        item {
            TextButton(onClick = { deleting = true }, modifier = Modifier.padding(horizontal = 8.dp)) {
                Text(text = "Delete rule", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (deleting) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text(text = "Delete ${rule.name.value}?") },
            text = { Text(text = "Its notifications stop being held. This can't be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleting = false
                        actions.onDelete()
                    },
                ) { Text(text = "Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text(text = "Cancel") } },
        )
    }
}
