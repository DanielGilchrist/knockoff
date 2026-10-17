package dev.danielgilchrist.knockoff.domain.notification

sealed interface ConversationTitle {
    data class Named(
        val name: ConversationName,
    ) : ConversationTitle

    data object Untitled : ConversationTitle

    companion object {
        fun of(candidates: List<String>): ConversationTitle =
            when (val title = candidates.map(String::trim).firstOrNull(String::isNotEmpty)) {
                null -> Untitled
                else -> Named(ConversationName(title))
            }
    }
}
