package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.ui.components.SupportingTone

sealed interface AppRowSupport {
    data object Missing : AppRowSupport

    data class Unpicked(
        val summary: String,
    ) : AppRowSupport

    data class Summary(
        val summary: String,
    ) : AppRowSupport

    val text: String
        get() =
            when (this) {
                Missing -> "Not installed"
                is Unpicked -> summary
                is Summary -> summary
            }

    val tone: SupportingTone
        get() =
            when (this) {
                Missing, is Summary -> SupportingTone.Plain
                is Unpicked -> SupportingTone.Problem
            }

    companion object {
        fun of(
            app: PackageName,
            missing: Boolean,
            draft: AppDraft,
            choices: AppChoices,
        ): AppRowSupport =
            when {
                missing -> {
                    Missing
                }

                draft.leavesNothingChosen(app) -> {
                    Unpicked(AppSummary.unpicked(choices.kind, choices.accounts.noun))
                }

                else -> {
                    Summary(AppSummary.of(draft, choices.accounts))
                }
            }
    }
}
