package dev.danielgilchrist.knockoff.domain.chat

import dev.danielgilchrist.knockoff.domain.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.FRIEND
import dev.danielgilchrist.knockoff.domain.notification.ConversationName
import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class ChatDecisionsTest {
    private val friendChat = COLLEAGUE_CHAT.copy(id = FRIEND, name = ConversationName("Friend"))

    @Test
    fun `decisions round-trip`() {
        val decisions =
            ChatDecisions.NONE.lettingThrough(COLLEAGUE_CHAT.key).askedAbout(setOf(COLLEAGUE_CHAT.key, friendChat.key))

        ChatDecisions.fromJson(decisions.toJson()) shouldBe ChatDecisionsDecode.Decoded(decisions)
    }

    @Test
    fun `the stored document is stable`() {
        ChatDecisions.NONE
            .lettingThrough(COLLEAGUE_CHAT.key)
            .askedAbout(setOf(friendChat.key))
            .toJson() shouldBe
            """
            {
                "version": 2,
                "letThrough": [
                    {
                        "app": "com.whatsapp",
                        "id": "1000000000001@lid"
                    }
                ],
                "asked": [
                    {
                        "app": "com.whatsapp",
                        "id": "1000000000002@lid"
                    }
                ]
            }
            """.trimIndent()
    }

    @Test
    fun `version 1 documents migrate with nothing asked`() {
        val v1 = """{"version": 1, "letThrough": [{"app": "com.whatsapp", "id": "1000000000001@lid"}]}"""

        ChatDecisions.fromJson(v1) shouldBe
            ChatDecisionsDecode.Decoded(ChatDecisions.NONE.lettingThrough(COLLEAGUE_CHAT.key))
    }

    @Test
    fun `other versions are unsupported and garbage is unreadable`() {
        val decode = ChatDecisions.fromJson("""{"version": 3, "letThrough": []}""")

        decode.shouldBeInstanceOf<ChatDecisionsDecode.UnsupportedVersion>().version shouldBe 3
        decode.shouldBeInstanceOf<ChatDecisionsDecode.Failure>().detail shouldBe "Unsupported chat decisions version 3"
        ChatDecisions
            .fromJson(
                "{nope",
            ).shouldBeInstanceOf<ChatDecisionsDecode.Unreadable>()
            .detail
            .isNotEmpty() shouldBe
            true
    }
}
