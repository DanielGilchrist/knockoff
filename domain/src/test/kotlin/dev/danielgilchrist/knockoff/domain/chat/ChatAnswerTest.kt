package dev.danielgilchrist.knockoff.domain.chat

import dev.danielgilchrist.knockoff.domain.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.FRIEND
import dev.danielgilchrist.knockoff.domain.ruleName
import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class ChatAnswerTest {
    private val friend = COLLEAGUE_CHAT.copy(id = FRIEND).key

    @Test
    fun `every answer round-trips`() {
        listOf(
            ChatAnswer.Hold(ruleName("Work"), COLLEAGUE_CHAT),
            ChatAnswer.LetThrough(COLLEAGUE_CHAT.key),
            ChatAnswer.Dismiss(setOf(COLLEAGUE_CHAT.key, friend)),
        ).forEach { answer ->
            ChatAnswer.fromJson(answer.toJson()) shouldBe ChatAnswerDecode.Decoded(answer)
        }
    }

    @Test
    fun `the stored document is stable`() {
        ChatAnswer.LetThrough(COLLEAGUE_CHAT.key).toJson() shouldBe
            """
            {
                "version": 1,
                "answer": {
                    "type": "letThrough",
                    "chat": {
                        "app": "com.whatsapp",
                        "id": "1000000000001@lid"
                    }
                }
            }
            """.trimIndent()
    }

    @Test
    fun `a hold needs a rule name and a whole chat`() {
        val chat = """{"app": "com.whatsapp", "channel": "chats", "id": "1@lid", "name": "Someone"}"""

        ChatAnswer.fromJson(hold(rule = " ", chat = chat)) shouldBe ChatAnswerDecode.Failed("blank rule name")
        ChatAnswer.fromJson(hold(rule = "Work", chat = chat.replace("Someone", ""))) shouldBe
            ChatAnswerDecode.Failed("blank chat name")
    }

    @Test
    fun `other versions and garbage fail`() {
        ChatAnswer.fromJson("""{"version": 2, "answer": {}}""") shouldBe
            ChatAnswerDecode.Failed("Unsupported chat answer version 2")
        ChatAnswer
            .fromJson("{nope")
            .shouldBeInstanceOf<ChatAnswerDecode.Failed>()
            .detail
            .isNotEmpty() shouldBe true
    }

    private fun hold(
        rule: String,
        chat: String,
    ): String = """{"version": 1, "answer": {"type": "hold", "rule": "$rule", "chat": $chat}}"""
}
