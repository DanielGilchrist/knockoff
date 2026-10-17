package dev.danielgilchrist.knockoff.domain.rule

sealed interface TextPatternParse {
    data class Valid(
        val pattern: TextPattern,
    ) : TextPatternParse

    data object Blank : TextPatternParse

    data class Invalid(
        val detail: String,
    ) : TextPatternParse
}
