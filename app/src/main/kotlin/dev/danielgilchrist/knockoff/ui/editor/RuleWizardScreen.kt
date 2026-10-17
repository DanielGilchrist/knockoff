package dev.danielgilchrist.knockoff.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.ui.Editing
import dev.danielgilchrist.knockoff.ui.RadioRow
import dev.danielgilchrist.knockoff.ui.TargetChoices

@Composable
internal fun RuleWizardScreen(
    guided: Editing.Guided,
    choices: TargetChoices,
    actions: WizardActions,
    modifier: Modifier = Modifier,
) {
    BackHandler {
        when (guided.step) {
            WizardStep.Goal -> actions.onCancel()
            WizardStep.What, WizardStep.When, WizardStep.Review -> actions.onGuide { it.toStep(it.step.previous()) }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "New rule · step ${guided.step.ordinal + 1} of ${WizardStep.entries.size}",
                style = MaterialTheme.typography.titleLarge,
            )
        }
        item {
            when (guided.step) {
                WizardStep.Goal -> GoalStep(goal = guided.goal, actions = actions)
                WizardStep.What -> WhatStep(guided = guided, choices = choices, onDraftChange = actions.onDraftChange)
                WizardStep.When -> WhenStep(guided = guided, actions = actions)
                WizardStep.Review -> ReviewStep(draft = guided.draft, onDraftChange = actions.onDraftChange)
            }
        }
        item { DraftProblems(problems = guided.shownProblems()) }
        item { WizardButtons(guided = guided, actions = actions) }
    }
}

@Composable
private fun GoalStep(
    goal: GoalChoice,
    actions: WizardActions,
) {
    Column {
        Text(text = "What should this rule do?", style = MaterialTheme.typography.titleMedium)
        RadioRow(
            text = "Keep work to work hours: hold notifications outside the hours you set",
            selected = goal == GoalChoice.Chosen(WizardGoal.WorkHours),
            onClick = { actions.onGuide { it.choosing(WizardGoal.WorkHours) } },
        )
        RadioRow(
            text = "Quiet time: hold notifications during the hours you set, such as evenings",
            selected = goal == GoalChoice.Chosen(WizardGoal.QuietTime),
            onClick = { actions.onGuide { it.choosing(WizardGoal.QuietTime) } },
        )
    }
}

@Composable
private fun WhatStep(
    guided: Editing.Guided,
    choices: TargetChoices,
    onDraftChange: (RuleDraft) -> Unit,
) {
    val draft = guided.draft

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Which notifications?", style = MaterialTheme.typography.titleMedium)
        AppsEditor(
            apps = choices.apps,
            selected = draft.apps,
            onSelectedChange = { onDraftChange(draft.copy(apps = it)) },
        )
        ChatsEditor(
            seen = choices.chats,
            apps = choices.apps,
            selected = draft.chats,
            onSelectedChange = { onDraftChange(draft.copy(chats = it)) },
        )
        GmailAccountsEditor(draft = draft, onDraftChange = onDraftChange)
        when {
            guided.picksSomething() -> {}

            else -> {
                Text(text = "Pick at least one app, chat or Gmail account.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun WhenStep(
    guided: Editing.Guided,
    actions: WizardActions,
) {
    Column {
        Text(text = "Which hours?", style = MaterialTheme.typography.titleMedium)
        guided.draft.windows.take(1).forEach { window ->
            WindowFields(window = window, onWindowChange = { hours -> actions.onGuide { it.withHours(hours) } })
        }
        when (val goal = guided.goal) {
            GoalChoice.Unchosen -> {}

            is GoalChoice.Chosen -> {
                when {
                    goal.goal.offersLunch -> {
                        LunchBreak(draft = guided.draft, onDraftChange = actions.onDraftChange)
                    }

                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun LunchBreak(
    draft: RuleDraft,
    onDraftChange: (RuleDraft) -> Unit,
) {
    CheckboxRow(
        title = "Take a lunch break",
        subtitle = "12:00 to 13:00 on the same days, when notifications are held too",
        checked = draft.breaks.isNotEmpty(),
        onToggle = {
            onDraftChange(
                when {
                    draft.breaks.isEmpty() -> draft.withBreakAdded()
                    else -> draft.copy(breaks = emptyList())
                },
            )
        },
    )
}

@Composable
private fun ReviewStep(
    draft: RuleDraft,
    onDraftChange: (RuleDraft) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Name your rule and check it", style = MaterialTheme.typography.titleMedium)
        RuleNameField(draft = draft, onDraftChange = onDraftChange)
        when (val checked = draft.parse()) {
            is Checked.Problems -> {}

            is Checked.Ok -> {
                RuleSummary(rule = checked.value)
            }
        }
    }
}

@Composable
private fun WizardButtons(
    guided: Editing.Guided,
    actions: WizardActions,
) {
    val step = guided.step

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (step) {
                WizardStep.Goal -> {
                    OutlinedButton(onClick = actions.onCancel) { Text(text = "Cancel") }
                }

                WizardStep.What, WizardStep.When, WizardStep.Review -> {
                    OutlinedButton(onClick = { actions.onGuide { it.toStep(step.previous()) } }) { Text(text = "Back") }
                }
            }
            when (step) {
                WizardStep.Goal, WizardStep.What, WizardStep.When -> {
                    Button(onClick = { actions.onGuide { it.toStep(step.next()) } }, enabled = guided.canContinue()) {
                        Text(text = "Next")
                    }
                }

                WizardStep.Review -> {
                    Button(onClick = actions.onCreate, enabled = guided.canContinue()) { Text(text = "Create rule") }
                }
            }
        }
        TextButton(onClick = { actions.onGuide { it.inFullEditor() } }) { Text(text = "Use the full editor instead") }
    }
}
