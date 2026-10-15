package dev.danielgilchrist.knockoff.channel

internal sealed interface Settlement {
    data object Settled : Settlement

    data class Unsettled(
        val detail: String,
    ) : Settlement

    fun and(other: Settlement): Settlement =
        when (this) {
            Settled -> other
            is Unsettled -> this
        }
}
