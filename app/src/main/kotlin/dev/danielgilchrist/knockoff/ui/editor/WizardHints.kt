package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.ui.Editing
import dev.danielgilchrist.knockoff.ui.describe

internal fun wizardHints(
    guided: Editing.Guided,
    shown: ProblemSplit,
    labelOf: (PackageName) -> String,
    pickOf: (PackageName) -> String,
): List<String> =
    when (guided.step) {
        WizardStep.Apps -> {
            when {
                !guided.picksSomething() -> listOf("Add at least one app.")
                else -> shown.unchosen.map { it.describe(labelOf, pickOf) }
            }
        }

        WizardStep.Goal, WizardStep.Hours, WizardStep.Review -> {
            emptyList()
        }
    }
