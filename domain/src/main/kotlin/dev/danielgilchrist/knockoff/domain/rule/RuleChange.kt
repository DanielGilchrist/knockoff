package dev.danielgilchrist.knockoff.domain.rule

sealed interface RuleChange {
    data object Pending : RuleChange

    data class Initial(
        val rules: RuleSet,
    ) : RuleChange

    data class Changed(
        val before: RuleSet,
        val after: RuleSet,
    ) : RuleChange

    fun next(rules: RuleSet): RuleChange =
        when (this) {
            Pending -> Initial(rules)
            is Initial -> Changed(before = this.rules, after = rules)
            is Changed -> Changed(before = after, after = rules)
        }
}
