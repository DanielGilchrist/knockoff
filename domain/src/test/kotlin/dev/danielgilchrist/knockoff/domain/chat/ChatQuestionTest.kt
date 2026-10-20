package dev.danielgilchrist.knockoff.domain.chat

import dev.danielgilchrist.knockoff.domain.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.ruleName
import dev.danielgilchrist.knockoff.domain.shouldBe
import org.junit.jupiter.api.Test

class ChatQuestionTest {
    @Test
    fun `a question's rules start with its first rule`() {
        ChatQuestion(COLLEAGUE_CHAT, ruleName("Work")).rules shouldBe listOf(ruleName("Work"))
        ChatQuestion(COLLEAGUE_CHAT, ruleName("Work"), listOf(ruleName("Clients"))).rules shouldBe
            listOf(ruleName("Work"), ruleName("Clients"))
    }
}
