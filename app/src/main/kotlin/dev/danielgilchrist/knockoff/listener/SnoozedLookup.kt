package dev.danielgilchrist.knockoff.listener

sealed interface SnoozedLookup {
    data object Snoozed : SnoozedLookup

    data object NotSnoozed : SnoozedLookup

    data class Unknown(
        val detail: String,
    ) : SnoozedLookup
}
