package dev.danielgilchrist.knockoff.ui.editor

enum class WizardStep {
    Goal,
    Apps,
    Hours,
    Review,
    ;

    val position: Int get() = ordinal + 1

    fun next(): WizardStep = entries.getOrElse(ordinal + 1) { this }

    fun previous(): WizardStep = entries.getOrElse(ordinal - 1) { this }

    companion object {
        fun of(problem: DraftProblem): WizardStep =
            when (problem) {
                is DraftProblem.NothingChosen, is DraftProblem.BlankPattern, is DraftProblem.InvalidPattern -> {
                    Apps
                }

                DraftProblem.NoHours,
                DraftProblem.BreaksCoverAllHours,
                is DraftProblem.HoursWithoutDays,
                is DraftProblem.BlankBreakName,
                is DraftProblem.BreakWithoutDays,
                -> {
                    Hours
                }

                DraftProblem.BlankName, DraftProblem.DuplicateName, is DraftProblem.NotSaved -> {
                    Review
                }
            }
    }
}
