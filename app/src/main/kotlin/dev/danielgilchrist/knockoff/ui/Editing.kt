package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.notification.AccountAddress
import dev.danielgilchrist.knockoff.domain.notification.AccountAddressParse
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.ui.editor.Checked
import dev.danielgilchrist.knockoff.ui.editor.DraftProblem
import dev.danielgilchrist.knockoff.ui.editor.GoalChoice
import dev.danielgilchrist.knockoff.ui.editor.RuleDraft
import dev.danielgilchrist.knockoff.ui.editor.WindowDraft
import dev.danielgilchrist.knockoff.ui.editor.WizardGoal
import dev.danielgilchrist.knockoff.ui.editor.WizardStep

sealed interface Editing {
    data object Closed : Editing

    data class Viewing(
        val rule: Rule,
    ) : Editing

    sealed interface Active : Editing {
        val target: EditTarget
        val draft: RuleDraft
        val problems: List<DraftProblem>

        fun saving(edited: Rule): Rule

        fun with(
            draft: RuleDraft,
            problems: List<DraftProblem>,
        ): Active
    }

    data class Section(
        val rule: Rule,
        val section: RuleSection,
        override val draft: RuleDraft,
        override val problems: List<DraftProblem>,
    ) : Active {
        companion object {
            fun of(
                rule: Rule,
                section: RuleSection,
            ): Section = Section(rule, section, RuleDraft.from(rule), emptyList())
        }

        override val target: EditTarget get() = EditTarget.Existing(rule)

        override fun saving(edited: Rule): Rule = section.merge(rule, edited)

        override fun with(
            draft: RuleDraft,
            problems: List<DraftProblem>,
        ): Active = copy(draft = draft, problems = problems)
    }

    data class Renaming(
        val rule: Rule,
        override val draft: RuleDraft,
        override val problems: List<DraftProblem>,
    ) : Active {
        companion object {
            fun of(rule: Rule): Renaming = Renaming(rule, RuleDraft.from(rule), emptyList())
        }

        override val target: EditTarget get() = EditTarget.Existing(rule)

        override fun saving(edited: Rule): Rule = rule.copy(name = edited.name)

        override fun with(
            draft: RuleDraft,
            problems: List<DraftProblem>,
        ): Active = copy(draft = draft, problems = problems)
    }

    data class Guided(
        override val draft: RuleDraft,
        override val problems: List<DraftProblem>,
        val step: WizardStep,
        val goal: GoalChoice,
    ) : Active {
        companion object {
            val START: Guided get() = Guided(RuleDraft.NEW, emptyList(), WizardStep.Goal, GoalChoice.Unchosen)
        }

        override val target: EditTarget get() = EditTarget.New

        override fun saving(edited: Rule): Rule = edited

        override fun with(
            draft: RuleDraft,
            problems: List<DraftProblem>,
        ): Active = copy(draft = draft, problems = problems)

        fun toStep(step: WizardStep): Guided = copy(step = step)

        fun choosing(chosen: WizardGoal): Guided =
            when (val current = goal) {
                GoalChoice.Unchosen -> replacingWith(chosen)
                is GoalChoice.Chosen -> if (current.goal == chosen) this else replacingWith(chosen)
            }

        fun withHours(window: WindowDraft): Guided =
            copy(
                draft =
                    draft.copy(
                        windows = listOf(window),
                        breaks =
                            draft.breaks.map {
                                it.copy(window = it.window.copy(days = window.days))
                            },
                    ),
                problems = emptyList(),
            )

        fun canContinue(): Boolean =
            when (step) {
                WizardStep.Goal -> goal != GoalChoice.Unchosen
                WizardStep.What -> picksSomething() && stepProblems().isEmpty()
                WizardStep.When, WizardStep.Review -> stepProblems().isEmpty()
            }

        fun picksSomething(): Boolean =
            listOf(draft.apps, draft.chats.keys, draft.gmailAccounts, draft.asking).any { it.isNotEmpty() } ||
                typesAnAccount()

        fun stepProblems(): List<DraftProblem> {
            val live =
                when (val checked = draft.parse()) {
                    is Checked.Ok -> emptyList()
                    is Checked.Problems -> checked.problems
                }

            return (live + problems).distinct().filter { WizardStep.of(it) == step }
        }

        fun shownProblems(): List<DraftProblem> = stepProblems().filterNot { it is DraftProblem.UnaddedAccount }

        private fun replacingWith(chosen: WizardGoal): Guided =
            copy(draft = chosen.replacing(goal, draft), problems = emptyList(), goal = GoalChoice.Chosen(chosen))

        private fun typesAnAccount(): Boolean =
            when (AccountAddress.parse(draft.pendingGmail)) {
                is AccountAddressParse.Valid -> true
                is AccountAddressParse.Invalid -> false
            }
    }
}
