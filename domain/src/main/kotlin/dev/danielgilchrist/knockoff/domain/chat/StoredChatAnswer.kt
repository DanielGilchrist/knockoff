package dev.danielgilchrist.knockoff.domain.chat

import dev.danielgilchrist.knockoff.domain.rule.RuleName
import dev.danielgilchrist.knockoff.domain.rule.RuleNameParse
import dev.danielgilchrist.knockoff.domain.settings.Parsed
import dev.danielgilchrist.knockoff.domain.settings.StoredChat
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface StoredChatAnswer {
    fun parse(): Parsed<ChatAnswer>

    @Serializable
    @SerialName("hold")
    data class Hold(
        private val rule: String,
        private val chat: StoredChat,
    ) : StoredChatAnswer {
        override fun parse(): Parsed<ChatAnswer> =
            when (val name = RuleName.parse(rule)) {
                is RuleNameParse.Valid -> chat.parse().then { Parsed.Ok(ChatAnswer.Hold(name.name, it)) }
                RuleNameParse.Blank -> Parsed.Failed("blank rule name")
            }
    }

    @Serializable
    @SerialName("letThrough")
    data class LetThrough(
        private val chat: StoredChatKey,
    ) : StoredChatAnswer {
        override fun parse(): Parsed<ChatAnswer> = Parsed.Ok(ChatAnswer.LetThrough(chat.toKey()))
    }

    @Serializable
    @SerialName("dismiss")
    data class Dismiss(
        private val chats: List<StoredChatKey>,
    ) : StoredChatAnswer {
        override fun parse(): Parsed<ChatAnswer> =
            Parsed.Ok(ChatAnswer.Dismiss(chats.map(StoredChatKey::toKey).toSet()))
    }

    companion object {
        fun from(answer: ChatAnswer): StoredChatAnswer =
            when (answer) {
                is ChatAnswer.Hold -> Hold(answer.rule.value, StoredChat.from(answer.chat))
                is ChatAnswer.LetThrough -> LetThrough(StoredChatKey.from(answer.chat))
                is ChatAnswer.Dismiss -> Dismiss(answer.chats.map(StoredChatKey::from))
            }
    }
}
