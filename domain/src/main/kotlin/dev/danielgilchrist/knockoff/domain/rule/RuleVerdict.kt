package dev.danielgilchrist.knockoff.domain.rule

sealed interface RuleVerdict {
    data object Abstains : RuleVerdict

    data object Permits : RuleVerdict

    data class Holds(
        val release: Release,
    ) : RuleVerdict
}
