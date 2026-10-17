package dev.danielgilchrist.knockoff.domain.chat

sealed interface ChatDecisionsDecode {
    data class Decoded(
        val decisions: ChatDecisions,
    ) : ChatDecisionsDecode

    sealed interface Failure : ChatDecisionsDecode {
        val detail: String
    }

    data class Unreadable(
        override val detail: String,
    ) : Failure

    data class UnsupportedVersion(
        val version: Int,
    ) : Failure {
        override val detail: String get() = "Unsupported chat decisions version $version"
    }
}
