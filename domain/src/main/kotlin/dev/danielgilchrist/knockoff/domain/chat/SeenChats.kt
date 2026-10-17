package dev.danielgilchrist.knockoff.domain.chat

import dev.danielgilchrist.knockoff.domain.json.VersionedDecode
import dev.danielgilchrist.knockoff.domain.json.VersionedJson
import dev.danielgilchrist.knockoff.domain.rule.RuleSet
import dev.danielgilchrist.knockoff.domain.rule.Target

@ConsistentCopyVisibility
data class SeenChats private constructor(
    val chats: List<Target.Chat>,
) {
    companion object {
        const val LIMIT = 200

        val NONE = SeenChats(emptyList())

        internal fun of(chats: List<Target.Chat>): SeenChats = SeenChats(chats.distinctBy { it.key }.take(LIMIT))

        fun fromJson(text: String): SeenChatsDecode =
            when (
                val decode =
                    VersionedJson.decode(text, mapOf(StoredSeenChats.CURRENT_VERSION to StoredSeenChats.serializer()))
            ) {
                is VersionedDecode.Decoded -> decode.value.toSeenChats()
                is VersionedDecode.Unreadable -> SeenChatsDecode.Unreadable(decode.detail)
                is VersionedDecode.UnsupportedVersion -> SeenChatsDecode.UnsupportedVersion(decode.version)
            }
    }

    fun seen(chat: Target.Chat): SeenChats = of(listOf(chat) + chats)

    fun expand(rules: RuleSet): RuleSet = RuleSet.expanded(rules, this)

    fun undecided(
        rules: RuleSet,
        decisions: ChatDecisions,
    ): List<Target.Chat> =
        chats.filter { chat ->
            rules.rules.none { it.holds(chat) } &&
                chat.key !in decisions.letThrough
        }

    fun questions(
        rules: RuleSet,
        decisions: ChatDecisions,
    ): List<ChatQuestion> =
        undecided(rules, decisions).filter { it.key !in decisions.asked }.flatMap { chat ->
            val asking = rules.rules.filter { it.asks(chat) }.map { it.name }

            when {
                asking.isEmpty() -> emptyList()
                else -> listOf(ChatQuestion(chat, asking))
            }
        }

    fun toJson(): String = VersionedJson.json.encodeToString(StoredSeenChats.from(this))
}
