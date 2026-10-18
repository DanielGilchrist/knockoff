package dev.danielgilchrist.knockoff.ask

import dev.danielgilchrist.knockoff.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.chat.ChatAnswer
import dev.danielgilchrist.knockoff.domain.chat.ChatQuestion
import dev.danielgilchrist.knockoff.domain.notification.ConversationId
import dev.danielgilchrist.knockoff.domain.notification.ConversationName
import dev.danielgilchrist.knockoff.domain.rule.RuleName
import dev.danielgilchrist.knockoff.domain.rule.RuleNameParse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class QuestionViewTest {
    private val friend = COLLEAGUE_CHAT.copy(id = ConversationId("2@lid"), name = ConversationName("Friend"))
    private val other = COLLEAGUE_CHAT.copy(id = ConversationId("3@lid"), name = ConversationName("Other"))
    private val rules = listOf(name("Work"), name("Clients"), name("Side"))

    @Test
    fun `a lone question offers holds with its first two rules and letting it through`() {
        val view = QuestionView.of(ChatQuestion(COLLEAGUE_CHAT, rules), emptyList()) { "WhatsApp" }

        assertEquals(
            QuestionView(
                title = "Colleague",
                text = "On WhatsApp. Hold it or let it through.",
                holds = rules.take(2).map { ChatAnswer.Hold(it, COLLEAGUE_CHAT) },
                letThrough = ChatAnswer.LetThrough(COLLEAGUE_CHAT.key),
                dismiss = ChatAnswer.Dismiss(setOf(COLLEAGUE_CHAT.key)),
            ),
            view,
        )
    }

    @Test
    fun `waiting questions are counted and dismissed together`() {
        val first = ChatQuestion(COLLEAGUE_CHAT, rules.take(1))
        val one = QuestionView.of(first, listOf(ChatQuestion(friend, rules))) { "WhatsApp" }
        val two = QuestionView.of(first, listOf(ChatQuestion(friend, rules), ChatQuestion(other, rules))) { "WhatsApp" }

        assertEquals("On WhatsApp. Hold it or let it through. 1 more is waiting.", one.text)
        assertEquals(
            "On WhatsApp. Hold it or let it through. 2 more are waiting.",
            two.text,
        )
        assertEquals(ChatAnswer.Dismiss(setOf(COLLEAGUE_CHAT.key, friend.key, other.key)), two.dismiss)
        assertEquals(listOf(ChatAnswer.Hold(name("Work"), COLLEAGUE_CHAT)), one.holds)
    }

    private fun name(raw: String): RuleName =
        assertInstanceOf(RuleNameParse.Valid::class.java, RuleName.parse(raw)).name
}
