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
import dev.danielgilchrist.knockoff.domain.rule.RuleApps
import dev.danielgilchrist.knockoff.ui.AppFocus
import dev.danielgilchrist.knockoff.ui.Editing
import dev.danielgilchrist.knockoff.ui.RuleSection
import dev.danielgilchrist.knockoff.ui.TargetChoices
import dev.danielgilchrist.knockoff.ui.components.Caption
import dev.danielgilchrist.knockoff.ui.components.ChoiceSegments
import dev.danielgilchrist.knockoff.ui.components.EditorScreen
import dev.danielgilchrist.knockoff.ui.components.SectionHeader
import dev.danielgilchrist.knockoff.ui.zone.DefaultZone

@Composable
internal fun RuleSectionScreen(
    editing: Editing.Section,
    choices: TargetChoices,
    defaultZone: DefaultZone,
    actions: RuleSectionActions,
    modifier: Modifier = Modifier,
) {
    when (val focus = editing.focus) {
        AppFocus.List -> {
            SectionEditor(
                editing = editing,
                choices = choices,
                defaultZone = defaultZone,
                actions = actions,
                modifier = modifier,
            )
        }

        is AppFocus.App -> {
            FocusedApp(
                app = focus.app,
                draft = editing.draft,
                stored = editing.rule.apps,
                choices = choices,
                onDraftChange = actions.onDraftChange,
                onFocus = actions.onFocus,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun SectionEditor(
    editing: Editing.Section,
    choices: TargetChoices,
    defaultZone: DefaultZone,
    actions: RuleSectionActions,
    modifier: Modifier = Modifier,
) {
    var discarding by rememberSaveable { mutableStateOf(false) }
    val back = { if (editing.changed) discarding = true else actions.onCancel() }

    BackHandler(onBack = back)

    EditorScreen(title = title(editing.section), onClose = back, onSave = actions.onSave, modifier = modifier) {
        if (editing.problems.isNotEmpty()) {
            item {
                DraftProblems(
                    problems = editing.problems,
                    draft = editing.draft,
                    stored = editing.rule.apps,
                    choices = choices,
                )
            }
        }
        section(editing.section, editing.draft, editing.rule.apps, choices, defaultZone, actions)
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
    stored: RuleApps,
    choices: TargetChoices,
    defaultZone: DefaultZone,
    actions: RuleSectionActions,
) {
    val change = actions.onDraftChange

    when (section) {
        RuleSection.Apps -> {
            item {
                AppsList(
                    draft = draft,
                    stored = stored,
                    choices = choices,
                    onDraftChange = change,
                    onOpenApp = { actions.onFocus(AppFocus.App(it)) },
                    use = AppsListUse.Editor,
                )
            }
        }

        RuleSection.Hours -> {
            item { PolicyChoice(policy = draft.policy, onPolicyChange = { change(draft.copy(policy = it)) }) }
            hoursItems(draft = draft, onDraftChange = change)
            breakItems(draft = draft, onDraftChange = change)
            item {
                ZoneEditor(zone = draft.zone, default = defaultZone, onZoneChange = { change(draft.copy(zone = it)) })
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
        RuleSection.Apps -> "Apps"
        RuleSection.Hours -> "Hours"
    }
