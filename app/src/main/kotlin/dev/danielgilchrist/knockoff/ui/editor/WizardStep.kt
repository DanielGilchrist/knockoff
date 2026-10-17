package dev.danielgilchrist.knockoff.ui.editor

enum class WizardStep {
    Goal,
    What,
    When,
    Review,
    ;

    val position: Int get() = ordinal + 1

    fun next(): WizardStep = entries.getOrElse(ordinal + 1) { this }

    fun previous(): WizardStep = entries.getOrElse(ordinal - 1) { this }

    companion object {
        fun of(problem: DraftProblem): WizardStep =
            when (problem) {
                is DraftProblem.UnaddedAccount, is DraftProblem.BlankPattern, is DraftProblem.InvalidPattern -> {
                    What
                }

                DraftProblem.NoHours,
                DraftProblem.BreaksCoverAllHours,
                is DraftProblem.HoursWithoutDays,
                is DraftProblem.BlankBreakName,
                is DraftProblem.BreakWithoutDays,
                -> {
                    When
                }

                DraftProblem.BlankName, DraftProblem.DuplicateName, is DraftProblem.NotSaved -> {
                    Review
                }
            }
    }
}
