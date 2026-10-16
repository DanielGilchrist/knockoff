package dev.danielgilchrist.knockoff.ui.editor

sealed interface DraftProblem {
    data object BlankName : DraftProblem

    data object DuplicateName : DraftProblem

    data object NoHours : DraftProblem

    data object BreaksCoverAllHours : DraftProblem

    data class HoursWithoutDays(
        val number: Int,
    ) : DraftProblem

    data class UnaddedAccount(
        val detail: String,
    ) : DraftProblem

    data class NotSaved(
        val detail: String,
    ) : DraftProblem
}
