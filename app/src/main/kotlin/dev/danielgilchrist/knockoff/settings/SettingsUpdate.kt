package dev.danielgilchrist.knockoff.settings

import dev.danielgilchrist.knockoff.domain.rule.RuleName

sealed interface SettingsUpdate {
    data object Saved : SettingsUpdate

    data class DuplicateNames(
        val names: Set<RuleName>,
    ) : SettingsUpdate {
        val detail: String get() = "Duplicate rule names ${names.map { it.value }}"
    }

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
