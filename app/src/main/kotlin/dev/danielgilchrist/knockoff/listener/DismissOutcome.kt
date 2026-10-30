package dev.danielgilchrist.knockoff.listener

internal sealed interface DismissOutcome {
    data object Dismissed : DismissOutcome

    data class Rejected(
        val detail: String,
    ) : DismissOutcome

    data class Unavailable(
        val detail: String,
    ) : DismissOutcome
}
