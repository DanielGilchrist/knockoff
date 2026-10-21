package dev.danielgilchrist.knockoff.domain.rule

sealed interface NarrowedParse {
    data class Valid(
        val scope: AppScope.Narrowed,
    ) : NarrowedParse

    data object NothingChosen : NarrowedParse
}
