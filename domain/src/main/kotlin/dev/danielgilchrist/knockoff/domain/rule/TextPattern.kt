package dev.danielgilchrist.knockoff.domain.rule

import java.util.regex.PatternSyntaxException

@ConsistentCopyVisibility
data class TextPattern private constructor(
    val source: String,
) {
    companion object {
        fun parse(source: String): TextPatternParse =
            when {
                source.isBlank() -> {
                    TextPatternParse.Blank
                }

                else -> {
                    try {
                        TextPatternParse.Valid(TextPattern(source))
                    } catch (exception: PatternSyntaxException) {
                        TextPatternParse.Invalid(exception.description)
                    }
                }
            }
    }

    private val regex = Regex(source)

    fun matches(text: String): Boolean = regex.containsMatchIn(text)
}
