package dev.danielgilchrist.knockoff.domain.chat

sealed interface SeenChatsDecode {
    data class Decoded(
        val seen: SeenChats,
    ) : SeenChatsDecode

    sealed interface Failure : SeenChatsDecode {
        val detail: String
    }

    data class Unreadable(
        override val detail: String,
    ) : Failure

    data class UnsupportedVersion(
        val version: Int,
    ) : Failure {
        override val detail: String get() = "Unsupported seen chats version $version"
    }

    data class Invalid(
        override val detail: String,
    ) : Failure
}
