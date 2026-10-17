package dev.danielgilchrist.knockoff.domain.chat

import dev.danielgilchrist.knockoff.domain.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class ChatDecisionsTest {
    @Test
    fun `letting a chat through records it`() {
        ChatDecisions.NONE.lettingThrough(COLLEAGUE_CHAT.key).letThrough shouldBe setOf(COLLEAGUE_CHAT.key)
    }

    @Test
    fun `decisions round-trip`() {
        val decisions = ChatDecisions.NONE.lettingThrough(COLLEAGUE_CHAT.key)

        ChatDecisions.fromJson(decisions.toJson()) shouldBe ChatDecisionsDecode.Decoded(decisions)
    }

    @Test
    fun `the stored document is stable`() {
        ChatDecisions.NONE.lettingThrough(COLLEAGUE_CHAT.key).toJson() shouldBe
            """
            {
                "version": 1,
                "letThrough": [
                    {
                        "app": "com.whatsapp",
                        "id": "1000000000001@lid"
                    }
                ]
            }
            """.trimIndent()
    }

    @Test
    fun `other versions are unsupported and garbage is unreadable`() {
        val decode = ChatDecisions.fromJson("""{"version": 2, "letThrough": []}""")

        decode.shouldBeInstanceOf<ChatDecisionsDecode.UnsupportedVersion>().version shouldBe 2
        decode.shouldBeInstanceOf<ChatDecisionsDecode.Failure>().detail shouldBe "Unsupported chat decisions version 2"
        ChatDecisions
            .fromJson(
                "{nope",
            ).shouldBeInstanceOf<ChatDecisionsDecode.Unreadable>()
            .detail
            .isNotEmpty() shouldBe
            true
    }
}
