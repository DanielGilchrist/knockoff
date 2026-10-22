package dev.danielgilchrist.knockoff.domain.settings

import dev.danielgilchrist.knockoff.domain.change.Baseline

sealed interface AppliedRulesDecode {
    data class Decoded(
        val baseline: Baseline.Known<AppliedRules>,
    ) : AppliedRulesDecode

    sealed interface Failure : AppliedRulesDecode {
        val detail: String
    }

    data class Unreadable(
        override val detail: String,
    ) : Failure

    data class UnsupportedVersion(
        val version: Int,
    ) : Failure {
        override val detail: String get() = "Unsupported applied rules version $version"
    }
}
