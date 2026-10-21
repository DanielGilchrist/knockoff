package dev.danielgilchrist.knockoff.domain.chat

import dev.danielgilchrist.knockoff.domain.group.KnownGroups
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
        groups: KnownGroups,
    ): List<Target.Chat> = asked(rules, decisions, groups).map { it.chat }

    fun questions(
        rules: RuleSet,
        decisions: ChatDecisions,
        groups: KnownGroups,
    ): List<ChatQuestion> = asked(rules, decisions, groups).filter { it.chat.key !in decisions.asked }

    private fun asked(
        rules: RuleSet,
        decisions: ChatDecisions,
        groups: KnownGroups,
    ): List<ChatQuestion> =
        chats
            .filter { chat ->
                val grouping = groups.groupingOf(chat)

                rules.rules.none { it.holds(chat, grouping) } && chat.key !in decisions.letThrough
            }.flatMap { chat ->
                val askers = rules.rules.filter { it.asks(chat) }.map { it.name }

                when {
                    askers.isEmpty() -> emptyList()
                    else -> listOf(ChatQuestion(chat, askers.first(), askers.drop(1)))
                }
            }

    fun toJson(): String = VersionedJson.json.encodeToString(StoredSeenChats.from(this))
}
