package dev.danielgilchrist.knockoff.domain.notification

@JvmInline
value class ConversationName(
    val value: String,
) {
    companion object {
        fun chosen(
            candidates: List<String>,
            id: ConversationId,
        ): ConversationName = ConversationName(candidates.map(String::trim).firstOrNull(String::isNotEmpty) ?: id.value)
    }
}
