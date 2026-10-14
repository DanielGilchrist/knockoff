package dev.danielgilchrist.knockoff.settings

import dev.danielgilchrist.knockoff.domain.rule.RuleName

sealed interface SettingsUpdate {
    data object Saved : SettingsUpdate

    data class DuplicateNames(
        val names: Set<RuleName>,
    ) : SettingsUpdate

    sealed interface Failure : SettingsUpdate {
        val detail: String
    }

    data class NotLoaded(
        override val detail: String,
    ) : Failure

    data class IoFailed(
        override val detail: String,
    ) : Failure
}
