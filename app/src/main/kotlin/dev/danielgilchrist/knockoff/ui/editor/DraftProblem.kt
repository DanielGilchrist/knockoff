package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.domain.notification.PackageName

sealed interface DraftProblem {
    data object BlankName : DraftProblem

    data object DuplicateName : DraftProblem

    data object NoHours : DraftProblem

    data object BreaksCoverAllHours : DraftProblem

    sealed interface MissingDays : DraftProblem

    data class HoursWithoutDays(
        val number: Int,
    ) : MissingDays

    data class BlankBreakName(
        val number: Int,
    ) : DraftProblem

    data class BreakWithoutDays(
        val number: Int,
    ) : MissingDays

    data class BlankPattern(
        val app: PackageName,
        val number: Int,
    ) : DraftProblem

    data class InvalidPattern(
        val app: PackageName,
        val number: Int,
        val detail: String,
    ) : DraftProblem

    data class NothingChosen(
        val app: PackageName,
    ) : DraftProblem

    data class NotSaved(
        val detail: String,
    ) : DraftProblem
}
