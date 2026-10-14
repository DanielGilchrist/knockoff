package dev.danielgilchrist.knockoff.domain.rule

sealed interface RuleNameParse {
    data class Valid(
        val name: RuleName,
    ) : RuleNameParse

    data object Blank : RuleNameParse
}
