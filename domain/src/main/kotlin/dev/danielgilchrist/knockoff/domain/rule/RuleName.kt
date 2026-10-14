package dev.danielgilchrist.knockoff.domain.rule

@JvmInline
value class RuleName private constructor(
    val value: String,
) {
    companion object {
        fun parse(raw: String): RuleNameParse {
            val trimmed = raw.trim()

            return when {
                trimmed.isEmpty() -> RuleNameParse.Blank
                else -> RuleNameParse.Valid(RuleName(trimmed))
            }
        }
    }
}
