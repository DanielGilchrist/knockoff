package dev.danielgilchrist.knockoff.listener

sealed interface SnoozeOutcome {
    data object Confirmed : SnoozeOutcome

    data class Rejected(
        val detail: String,
    ) : SnoozeOutcome

    data class Unconfirmed(
        val detail: String,
    ) : SnoozeOutcome
}
