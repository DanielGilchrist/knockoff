package dev.danielgilchrist.knockoff.listener

sealed interface ReleaseOutcome {
    data object Released : ReleaseOutcome

    data class Rejected(
        val detail: String,
    ) : ReleaseOutcome

    data class Unconfirmed(
        val detail: String,
    ) : ReleaseOutcome
}
