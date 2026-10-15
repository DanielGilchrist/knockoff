package dev.danielgilchrist.knockoff.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.ui.EditTarget
import dev.danielgilchrist.knockoff.ui.Editing
import dev.danielgilchrist.knockoff.ui.InstalledApps
import dev.danielgilchrist.knockoff.ui.RadioRow
import dev.danielgilchrist.knockoff.ui.describe
import dev.danielgilchrist.knockoff.ui.home.RuleStateSwitch
import dev.danielgilchrist.knockoff.ui.removedAt
import dev.danielgilchrist.knockoff.ui.replacedAt

@Composable
internal fun RuleEditorScreen(
    editing: Editing.Open,
    apps: InstalledApps,
    actions: RuleEditorActions,
    modifier: Modifier = Modifier,
) {
    val draft = editing.draft
    val change = actions.onDraftChange

    BackHandler(onBack = actions.onCancel)

    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { NameAndState(draft = draft, onDraftChange = change) }
        item { PolicyChoice(policy = draft.policy, onPolicyChange = { change(draft.copy(policy = it)) }) }
        itemsIndexed(draft.windows) { index, window ->
            WindowEditor(
                number = index + 1,
                window = window,
                onWindowChange = { change(draft.copy(windows = draft.windows.replacedAt(index, it))) },
                onRemove = { change(draft.copy(windows = draft.windows.removedAt(index))) },
            )
        }
        item {
            OutlinedButton(onClick = { change(draft.copy(windows = draft.windows + WindowDraft.WORKING_HOURS)) }) {
                Text(text = "Add hours")
            }
        }
        item { ZoneEditor(zone = draft.zone, onZoneChange = { change(draft.copy(zone = it)) }) }
        item { AppsEditor(apps = apps, selected = draft.apps, onSelectedChange = { change(draft.copy(apps = it)) }) }
        item { GmailAccountsEditor(draft = draft, onDraftChange = change) }
        item { Problems(problems = editing.problems) }
        item { EditorButtons(target = editing.target, actions = actions) }
    }
}

@Composable
private fun NameAndState(
    draft: RuleDraft,
    onDraftChange: (RuleDraft) -> Unit,
) {
    Column {
        OutlinedTextField(
            value = draft.name,
            onValueChange = { onDraftChange(draft.copy(name = it)) },
            label = { Text(text = "Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Enabled", modifier = Modifier.weight(1f))
            RuleStateSwitch(state = draft.state, onStateChange = { onDraftChange(draft.copy(state = it)) })
        }
    }
}

@Composable
private fun PolicyChoice(
    policy: Policy,
    onPolicyChange: (Policy) -> Unit,
) {
    Column {
        Policy.entries.forEach { option ->
            RadioRow(text = option.describe(), selected = policy == option, onClick = { onPolicyChange(option) })
        }
    }
}

@Composable
private fun Problems(problems: List<DraftProblem>) {
    Column {
        problems.forEach { Text(text = it.describe(), color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun EditorButtons(
    target: EditTarget,
    actions: RuleEditorActions,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = actions.onSave) { Text(text = "Save") }
        OutlinedButton(onClick = actions.onCancel) { Text(text = "Cancel") }
        when (target) {
            EditTarget.New -> {}

            is EditTarget.Existing -> {
                TextButton(onClick = { actions.onDelete(target) }) { Text(text = "Delete") }
            }
        }
    }
}
