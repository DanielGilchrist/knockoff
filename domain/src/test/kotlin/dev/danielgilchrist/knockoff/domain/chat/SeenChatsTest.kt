package dev.danielgilchrist.knockoff.domain.chat

import dev.danielgilchrist.knockoff.domain.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.FRIEND
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.ConversationId
import dev.danielgilchrist.knockoff.domain.notification.ConversationName
import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class SeenChatsTest {
    private val friend = COLLEAGUE_CHAT.copy(id = FRIEND, name = ConversationName("Friend"))

    @Test
    fun `the latest chat comes first`() {
        SeenChats.NONE
            .seen(COLLEAGUE_CHAT)
            .seen(friend)
            .chats shouldBe listOf(friend, COLLEAGUE_CHAT)
    }

    @Test
    fun `seeing a chat again moves it first with its latest name and channel`() {
        val renamed =
            COLLEAGUE_CHAT.copy(
                channel = ChannelId("individual_chat_defaults_5"),
                name = ConversationName("Boss"),
            )

        SeenChats.NONE
            .seen(COLLEAGUE_CHAT)
            .seen(friend)
            .seen(renamed)
            .chats shouldBe listOf(renamed, friend)
    }

    @Test
    fun `only the latest chats are kept`() {
        val many =
            (1..SeenChats.LIMIT + 1).fold(SeenChats.NONE) { seen, number ->
                seen.seen(COLLEAGUE_CHAT.copy(id = ConversationId("$number@lid")))
            }

        many.chats.size shouldBe SeenChats.LIMIT
        many.chats.first().id shouldBe ConversationId("${SeenChats.LIMIT + 1}@lid")
        many.chats.last().id shouldBe ConversationId("2@lid")
    }

    @Test
    fun `seen chats round-trip`() {
        val seen = SeenChats.NONE.seen(COLLEAGUE_CHAT).seen(friend)

        SeenChats.fromJson(seen.toJson()) shouldBe SeenChatsDecode.Decoded(seen)
    }

    @Test
    fun `the stored document is stable`() {
        SeenChats.NONE.seen(COLLEAGUE_CHAT).toJson() shouldBe
            """
            {
                "version": 1,
                "chats": [
                    {
                        "app": "com.whatsapp",
                        "channel": "individual_chat_defaults_4",
                        "id": "1000000000001@lid",
                        "name": "Colleague"
                    }
                ]
            }
            """.trimIndent()
    }

    @Test
    fun `an invalid chat is reported`() {
        val chat = """{"app": "com.whatsapp", "channel": "chats", "id": " ", "name": "Someone"}"""

        SeenChats
            .fromJson("""{"version": 1, "chats": [$chat]}""")
            .shouldBeInstanceOf<SeenChatsDecode.Invalid>()
            .detail shouldBe "chat 1: blank chat id"
    }

    @Test
    fun `other versions are unsupported`() {
        val decode = SeenChats.fromJson("""{"version": 2, "chats": []}""")

        decode.shouldBeInstanceOf<SeenChatsDecode.UnsupportedVersion>().version shouldBe 2
        decode.shouldBeInstanceOf<SeenChatsDecode.Failure>().detail shouldBe "Unsupported seen chats version 2"
    }

    @Test
    fun `an unparseable document is unreadable`() {
        val detail = SeenChats.fromJson("{nope").shouldBeInstanceOf<SeenChatsDecode.Unreadable>().detail

        detail.take("kotlinx.serialization.".length) shouldBe "kotlinx.serialization."
    }
}
