package dev.danielgilchrist.knockoff.domain.rule

sealed interface RuleSetParse {
    data class Valid(
        val rules: RuleSet,
    ) : RuleSetParse

    data class DuplicateNames(
        val names: Set<RuleName>,
    ) : RuleSetParse
}
