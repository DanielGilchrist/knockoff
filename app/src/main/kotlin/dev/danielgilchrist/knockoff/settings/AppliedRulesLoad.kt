package dev.danielgilchrist.knockoff.settings

import dev.danielgilchrist.knockoff.domain.change.Baseline
import dev.danielgilchrist.knockoff.domain.settings.AppliedRules

sealed interface AppliedRulesLoad {
    data object Missing : AppliedRulesLoad

    data class Loaded(
        val stored: Baseline.Known<AppliedRules>,
    ) : AppliedRulesLoad

    data class Failed(
        val detail: String,
    ) : AppliedRulesLoad

    val baseline: Baseline<AppliedRules>
        get() =
            when (this) {
                Missing, is Failed -> Baseline.Unknown
                is Loaded -> stored
            }
}
