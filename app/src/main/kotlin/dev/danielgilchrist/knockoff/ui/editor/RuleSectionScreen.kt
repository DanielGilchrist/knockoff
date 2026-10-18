package dev.danielgilchrist.knockoff.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.ui.Editing
import dev.danielgilchrist.knockoff.ui.RuleSection
import dev.danielgilchrist.knockoff.ui.TargetChoices
import dev.danielgilchrist.knockoff.ui.components.Caption
import dev.danielgilchrist.knockoff.ui.components.ChoiceSegments
import dev.danielgilchrist.knockoff.ui.components.EditorScreen
import dev.danielgilchrist.knockoff.ui.components.SectionHeader

@Composable
internal fun RuleSectionScreen(
    editing: Editing.Section,
    choices: TargetChoices,
    actions: RuleSectionActions,
    modifier: Modifier = Modifier,
) {
    var discarding by rememberSaveable { mutableStateOf(false) }
    val back = { if (editing.draft == RuleDraft.from(editing.rule)) actions.onCancel() else discarding = true }

    BackHandler(onBack = back)

    EditorScreen(title = title(editing.section), onClose = back, onSave = actions.onSave, modifier = modifier) {
        if (editing.problems.isNotEmpty()) {
            item { DraftProblems(problems = editing.problems) }
        }
        section(editing.section, editing.draft, choices, actions.onDraftChange)
    }

    if (discarding) {
        AlertDialog(
            onDismissRequest = { discarding = false },
            title = { Text(text = "Discard changes?") },
            text = { Text(text = "Your changes to this section haven't been saved.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        discarding = false
                        actions.onCancel()
                    },
                ) { Text(text = "Discard") }
            },
            dismissButton = { TextButton(onClick = { discarding = false }) { Text(text = "Keep editing") } },
        )
    }
}

private fun LazyListScope.section(
    section: RuleSection,
    draft: RuleDraft,
    choices: TargetChoices,
    change: (RuleDraft) -> Unit,
) {
    when (section) {
        RuleSection.Name -> {
            item { RuleNameField(draft = draft, onDraftChange = change) }
        }

        RuleSection.What -> {
            item {
                AppsEditor(
                    apps = choices.apps,
                    selected = draft.apps,
                    onSelectedChange = { change(draft.copy(apps = it)) },
                )
            }
            item { ChatsEditor(seen = choices.chats, apps = choices.apps, draft = draft, onDraftChange = change) }
            item { GmailAccountsEditor(draft = draft, onDraftChange = change) }
        }

        RuleSection.When -> {
            item { PolicyChoice(policy = draft.policy, onPolicyChange = { change(draft.copy(policy = it)) }) }
            hoursItems(draft = draft, onDraftChange = change)
            breakItems(draft = draft, onDraftChange = change)
        }

        RuleSection.Advanced -> {
            item { ZoneEditor(zone = draft.zone, onZoneChange = { change(draft.copy(zone = it)) }) }
            item {
                PatternsEditor(
                    choices = choices,
                    patterns = draft.patterns,
                    onPatternsChange = { change(draft.copy(patterns = it)) },
                )
            }
        }
    }
}

@Composable
private fun PolicyChoice(
    policy: Policy,
    onPolicyChange: (Policy) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(text = "During these hours")
        ChoiceSegments(
            options = Policy.entries,
            selected = policy,
            label = {
                when (it) {
                    Policy.DeliverOnlyDuring -> "Let through"
                    Policy.HoldDuring -> "Hold"
                }
            },
            onSelect = onPolicyChange,
        )
        Caption(
            text =
                when (policy) {
                    Policy.DeliverOnlyDuring -> "Held at all other times."
                    Policy.HoldDuring -> "Let through at all other times."
                },
        )
    }
}

private fun title(section: RuleSection): String =
    when (section) {
        RuleSection.Name -> "Name"
        RuleSection.What -> "What it holds"
        RuleSection.When -> "When"
        RuleSection.Advanced -> "Advanced"
    }
