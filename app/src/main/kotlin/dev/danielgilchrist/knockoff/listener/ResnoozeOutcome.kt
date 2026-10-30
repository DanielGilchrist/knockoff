package dev.danielgilchrist.knockoff.listener

sealed interface ResnoozeOutcome {
    data object Requested : ResnoozeOutcome

    data class Rejected(
        val detail: String,
    ) : ResnoozeOutcome

    data class Unavailable(
        val detail: String,
    ) : ResnoozeOutcome
}
