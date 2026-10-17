package dev.danielgilchrist.knockoff.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.ui.Editing
import dev.danielgilchrist.knockoff.ui.TargetChoices

@Composable
internal fun RuleWizardScreen(
    guided: Editing.Guided,
    choices: TargetChoices,
    actions: WizardActions,
    modifier: Modifier = Modifier,
) {
    BackHandler { goBack(guided.step, actions) }

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        WizardHeader(step = guided.step, onFullEditor = { actions.onGuide { it.inFullEditor() } })
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                when (guided.step) {
                    WizardStep.Goal -> {
                        GoalStep(goal = guided.goal, actions = actions)
                    }

                    WizardStep.What -> {
                        WhatStep(
                            guided = guided,
                            choices = choices,
                            onDraftChange = actions.onDraftChange,
                        )
                    }

                    WizardStep.When -> {
                        WhenStep(guided = guided, actions = actions)
                    }

                    WizardStep.Review -> {
                        ReviewStep(draft = guided.draft, onDraftChange = actions.onDraftChange)
                    }
                }
            }
            item { DraftProblems(problems = guided.shownProblems()) }
        }
        WizardButtons(guided = guided, actions = actions)
    }
}

@Composable
private fun WizardHeader(
    step: WizardStep,
    onFullEditor: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Step ${step.position} of ${WizardStep.entries.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onFullEditor) { Text(text = "Switch to full editor") }
        }
        LinearProgressIndicator(
            progress = { step.position / WizardStep.entries.size.toFloat() },
            modifier = Modifier.fillMaxWidth().padding(end = 12.dp),
        )
        Text(
            text = title(step),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 20.dp),
        )
    }
}

private fun title(step: WizardStep): String =
    when (step) {
        WizardStep.Goal -> "What should this rule do?"
        WizardStep.What -> "Which notifications?"
        WizardStep.When -> "Which hours?"
        WizardStep.Review -> "Name your rule and check it"
    }

@Composable
private fun GoalStep(
    goal: GoalChoice,
    actions: WizardActions,
) {
    Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChoiceCard(
            title = "Keep work to work hours",
            description = "Hold notifications outside the hours you set, so work stays at work.",
            selected = goal == GoalChoice.Chosen(WizardGoal.WorkHours),
            onClick = { actions.onGuide { it.choosing(WizardGoal.WorkHours) } },
        )
        ChoiceCard(
            title = "Quiet time",
            description = "Hold notifications during the hours you set, such as evenings and nights.",
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

    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { goBack(step, actions) }) {
                Text(
                    text =
                        when (step) {
                            WizardStep.Goal -> "Cancel"
                            WizardStep.What, WizardStep.When, WizardStep.Review -> "Back"
                        },
                )
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
    }
}

private fun goBack(
    step: WizardStep,
    actions: WizardActions,
) {
    when (step) {
        WizardStep.Goal -> actions.onCancel()
        WizardStep.What, WizardStep.When, WizardStep.Review -> actions.onGuide { it.toStep(step.previous()) }
    }
}
