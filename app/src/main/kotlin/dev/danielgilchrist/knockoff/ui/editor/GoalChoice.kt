package dev.danielgilchrist.knockoff.ui.editor

sealed interface GoalChoice {
    data object Unchosen : GoalChoice

    data class Chosen(
        val goal: WizardGoal,
    ) : GoalChoice
}
