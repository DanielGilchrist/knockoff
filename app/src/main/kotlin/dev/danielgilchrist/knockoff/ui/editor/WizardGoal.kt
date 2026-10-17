package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.domain.rule.Policy
import java.time.DayOfWeek
import java.time.LocalTime

enum class WizardGoal {
    WorkHours,
    QuietTime,
    ;

    val defaultName: String
        get() =
            when (this) {
                WorkHours -> "Work"
                QuietTime -> "Quiet time"
            }

    val policy: Policy
        get() =
            when (this) {
                WorkHours -> Policy.DeliverOnlyDuring
                QuietTime -> Policy.HoldDuring
            }

    val hours: WindowDraft
        get() =
            when (this) {
                WorkHours -> WindowDraft.WORKING_HOURS
                QuietTime -> QUIET_HOURS
            }

    val offersLunch: Boolean
        get() =
            when (this) {
                WorkHours -> true
                QuietTime -> false
            }

    fun replacing(
        previous: GoalChoice,
        draft: RuleDraft,
    ): RuleDraft {
        val (previousName, previousHours) =
            when (previous) {
                GoalChoice.Unchosen -> RuleDraft.NEW.name to RuleDraft.NEW.windows
                is GoalChoice.Chosen -> previous.goal.defaultName to listOf(previous.goal.hours)
            }

        val name =
            when {
                draft.name.isBlank() || draft.name == previousName -> defaultName
                else -> draft.name
            }

        val windows =
            when {
                draft.windows == previousHours -> listOf(hours)
                else -> draft.windows
            }
        val breaks =
            when {
                !offersLunch || draft.windows == previousHours -> emptyList()
                else -> draft.breaks
            }

        return draft.copy(name = name, policy = policy, windows = windows, breaks = breaks)
    }

    companion object {
        private val QUIET_HOURS = WindowDraft(DayOfWeek.entries.toSet(), LocalTime.of(21, 0), LocalTime.of(7, 0))
    }
}
