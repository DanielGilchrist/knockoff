package dev.danielgilchrist.knockoff.settings

sealed interface AppliedRulesSave {
    data object Saved : AppliedRulesSave

    data class Failed(
        val detail: String,
    ) : AppliedRulesSave
}
