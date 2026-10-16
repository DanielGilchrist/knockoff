package dev.danielgilchrist.knockoff.domain.schedule

sealed interface BreakNameParse {
    data class Valid(
        val name: BreakName,
    ) : BreakNameParse

    data object Blank : BreakNameParse
}
