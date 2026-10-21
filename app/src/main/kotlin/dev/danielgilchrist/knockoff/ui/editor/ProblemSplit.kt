package dev.danielgilchrist.knockoff.ui.editor

data class ProblemSplit(
    val unchosen: List<DraftProblem.NothingChosen>,
    val others: List<DraftProblem>,
) {
    companion object {
        val NONE: ProblemSplit get() = ProblemSplit(emptyList(), emptyList())

        fun of(problems: List<DraftProblem>): ProblemSplit = problems.fold(NONE, ProblemSplit::with)

        fun of(checked: Checked<*>): ProblemSplit =
            when (checked) {
                is Checked.Ok -> NONE
                is Checked.Problems -> of(checked.problems)
            }
    }

    private fun with(problem: DraftProblem): ProblemSplit =
        when (problem) {
            is DraftProblem.NothingChosen -> {
                copy(unchosen = unchosen + problem)
            }

            DraftProblem.BlankName,
            DraftProblem.DuplicateName,
            DraftProblem.NoHours,
            DraftProblem.BreaksCoverAllHours,
            is DraftProblem.HoursWithoutDays,
            is DraftProblem.BlankBreakName,
            is DraftProblem.BreakWithoutDays,
            is DraftProblem.BlankPattern,
            is DraftProblem.InvalidPattern,
            is DraftProblem.NotSaved,
            -> {
                copy(others = others + problem)
            }
        }
}
