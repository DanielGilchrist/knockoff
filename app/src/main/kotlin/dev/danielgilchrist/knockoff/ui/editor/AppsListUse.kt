package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.ui.GrammaticalNumber
import dev.danielgilchrist.knockoff.ui.HeldPhrase
import dev.danielgilchrist.knockoff.ui.HeldPhrases
import dev.danielgilchrist.knockoff.ui.hoursOf

enum class AppsListUse {
    Editor,
    Wizard,
    ;

    fun caption(draft: RuleDraft): String =
        when (this) {
            Editor -> {
                HeldPhrases.effect(
                    HeldPhrase.of("These apps", GrammaticalNumber.Many),
                    draft.policy,
                    "during ${hoursOf(draft.name)}",
                )
            }

            Wizard -> {
                when (draft.policy) {
                    Policy.DeliverOnlyDuring -> "They'll only reach you during the hours you set next."
                    Policy.HoldDuring -> "They'll go quiet during the hours you set next."
                }
            }
        }

    val saysWhenEmpty: Boolean
        get() =
            when (this) {
                Editor -> true
                Wizard -> false
            }
}
