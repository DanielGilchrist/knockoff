package dev.danielgilchrist.knockoff.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.R
import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.ui.Editing
import dev.danielgilchrist.knockoff.ui.InstalledApps
import dev.danielgilchrist.knockoff.ui.RuleOverview
import dev.danielgilchrist.knockoff.ui.TargetChoices
import dev.danielgilchrist.knockoff.ui.components.Caption
import dev.danielgilchrist.knockoff.ui.components.CardRemoval
import dev.danielgilchrist.knockoff.ui.components.EditCard
import dev.danielgilchrist.knockoff.ui.components.ListGroup
import dev.danielgilchrist.knockoff.ui.components.NavigationListItem
import dev.danielgilchrist.knockoff.ui.components.SwitchListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RuleWizardScreen(
    guided: Editing.Guided,
    choices: TargetChoices,
    actions: WizardActions,
    modifier: Modifier = Modifier,
) {
    var discarding by rememberSaveable { mutableStateOf(false) }
    val shown = guided.shownProblems()
    val close = { if (guided.draft == RuleDraft.NEW) actions.onCancel() else discarding = true }

    BackHandler { goBack(guided.step, actions, close) }

    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(text = "New rule") },
                    navigationIcon = {
                        IconButton(onClick = close) {
                            Icon(painter = painterResource(R.drawable.ic_close), contentDescription = "Discard")
                        }
                    },
                )
                LinearProgressIndicator(
                    progress = { guided.step.position / WizardStep.entries.size.toFloat() },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .semantics {
                                stateDescription = "Step ${guided.step.position} of ${WizardStep.entries.size}"
                            },
                )
            }
        },
        bottomBar = { WizardButtons(guided = guided, actions = actions, onClose = close) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = title(guided.step),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            if (shown.isNotEmpty()) {
                item { DraftProblems(problems = shown, title = "Fix before carrying on") }
            }
            item { WizardStepContent(guided = guided, choices = choices, actions = actions) }
        }
    }

    if (discarding) {
        DiscardDialog(
            onDiscard = {
                discarding = false
                actions.onCancel()
            },
            onKeep = { discarding = false },
        )
    }
}

@Composable
private fun DiscardDialog(
    onDiscard: () -> Unit,
    onKeep: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onKeep,
        title = { Text(text = "Discard this rule?") },
        confirmButton = { TextButton(onClick = onDiscard) { Text(text = "Discard") } },
        dismissButton = { TextButton(onClick = onKeep) { Text(text = "Keep editing") } },
    )
}

@Composable
private fun WizardStepContent(
    guided: Editing.Guided,
    choices: TargetChoices,
    actions: WizardActions,
) {
    when (guided.step) {
        WizardStep.Goal -> GoalStep(goal = guided.goal, actions = actions)
        WizardStep.What -> WhatStep(guided = guided, choices = choices, onDraftChange = actions.onDraftChange)
        WizardStep.When -> WhenStep(guided = guided, actions = actions)
        WizardStep.Review -> ReviewStep(draft = guided.draft, apps = choices.apps, actions = actions)
    }
}

private fun title(step: WizardStep): String =
    when (step) {
        WizardStep.Goal -> "What should this rule do?"
        WizardStep.What -> "Which notifications?"
        WizardStep.When -> "Which hours?"
        WizardStep.Review -> "Check it over"
    }

@Composable
private fun GoalStep(
    goal: GoalChoice,
    actions: WizardActions,
) {
    Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ChoiceCard(
            icon = R.drawable.ic_work,
            title = "Keep work to work hours",
            description = "Hold notifications outside the hours you set, so work stays at work.",
            selected = goal == GoalChoice.Chosen(WizardGoal.WorkHours),
            onClick = { actions.onGuide { it.choosing(WizardGoal.WorkHours) } },
        )
        ChoiceCard(
            icon = R.drawable.ic_bedtime,
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

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AppsEditor(
            apps = choices.apps,
            selected = draft.apps,
            onSelectedChange = { onDraftChange(draft.copy(apps = it)) },
        )
        ChatsEditor(seen = choices.chats, apps = choices.apps, draft = draft, onDraftChange = onDraftChange)
        GmailAccountsEditor(draft = draft, onDraftChange = onDraftChange)
    }
}

@Composable
private fun WhenStep(
    guided: Editing.Guided,
    actions: WizardActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Caption(
            text =
                when (guided.draft.policy) {
                    Policy.DeliverOnlyDuring -> "Notifications are let through only in these hours."
                    Policy.HoldDuring -> "Notifications are held in these hours."
                },
        )
        guided.draft.windows.take(1).forEach { window ->
            EditCard(title = "Hours", removal = CardRemoval.None) {
                WindowFields(window = window, onWindowChange = { hours -> actions.onGuide { it.withHours(hours) } })
            }
        }
        when (val goal = guided.goal) {
            GoalChoice.Unchosen -> {}

            is GoalChoice.Chosen -> {
                if (goal.goal.offersLunch) LunchBreak(draft = guided.draft, onDraftChange = actions.onDraftChange)
            }
        }
    }
}

@Composable
private fun LunchBreak(
    draft: RuleDraft,
    onDraftChange: (RuleDraft) -> Unit,
) {
    ListGroup {
        SwitchListItem(
            headline = "Lunch break",
            supporting = "12:00 to 13:00 on the same days, held too",
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
}

@Composable
private fun ReviewStep(
    draft: RuleDraft,
    apps: InstalledApps,
    actions: WizardActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        RuleNameField(draft = draft, onDraftChange = actions.onDraftChange)
        when (val checked = draft.parse()) {
            is Checked.Problems -> {}

            is Checked.Ok -> {
                val overview = RuleOverview.of(checked.value, apps::labelOf)

                Text(text = overview.sentence, style = MaterialTheme.typography.bodyLarge)
                ListGroup {
                    NavigationListItem(
                        icon = R.drawable.ic_apps,
                        headline = "What",
                        supporting = overview.what,
                        onClick = { actions.onGuide { it.toStep(WizardStep.What) } },
                    )
                    NavigationListItem(
                        icon = R.drawable.ic_schedule,
                        headline = "When",
                        supporting = overview.hours,
                        onClick = { actions.onGuide { it.toStep(WizardStep.When) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun WizardButtons(
    guided: Editing.Guided,
    actions: WizardActions,
    onClose: () -> Unit,
) {
    val step = guided.step

    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            modifier =
                Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            when (step) {
                WizardStep.What -> {
                    if (!guided.picksSomething()) {
                        Caption(
                            text = "Pick at least one app, chat or Gmail account.",
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }

                WizardStep.Goal, WizardStep.When, WizardStep.Review -> {}
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (step) {
                    WizardStep.Goal -> {
                        Spacer(modifier = Modifier)
                    }

                    WizardStep.What, WizardStep.When, WizardStep.Review -> {
                        TextButton(onClick = { goBack(step, actions, onClose) }) { Text(text = "Back") }
                    }
                }
                when (step) {
                    WizardStep.Goal, WizardStep.What, WizardStep.When -> {
                        Button(
                            onClick = { actions.onGuide { it.toStep(step.next()) } },
                            enabled = guided.canContinue(),
                        ) {
                            Text(text = "Next")
                        }
                    }

                    WizardStep.Review -> {
                        Button(
                            onClick = actions.onCreate,
                            enabled = guided.canContinue(),
                        ) { Text(text = "Create rule") }
                    }
                }
            }
        }
    }
}

private fun goBack(
    step: WizardStep,
    actions: WizardActions,
    onClose: () -> Unit,
) {
    when (step) {
        WizardStep.Goal -> onClose()
        WizardStep.What, WizardStep.When, WizardStep.Review -> actions.onGuide { it.toStep(step.previous()) }
    }
}
