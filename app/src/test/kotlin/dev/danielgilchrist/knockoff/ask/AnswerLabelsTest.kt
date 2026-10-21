package dev.danielgilchrist.knockoff.ask

import dev.danielgilchrist.knockoff.domain.notification.ConversationName
import dev.danielgilchrist.knockoff.domain.rule.RuleState
import dev.danielgilchrist.knockoff.domain.settings.Settings
import dev.danielgilchrist.knockoff.ui.GrammaticalNumber
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AnswerLabelsTest {
    private val work =
        Settings.DEFAULT.rules.rules
            .single()

    @Test
    fun `adding a chat names the rule and says when it's off`() {
        assertEquals("Add to Work", AnswerLabels.addTo(work.name))
        assertEquals("Add to Work", AnswerLabels.addTo(work))
        assertEquals("Add to Work (off)", AnswerLabels.addTo(work.copy(state = RuleState.Disabled)))
    }

    @Test
    fun `adding or letting through follows how many chats there are`() {
        assertEquals("Add it to a rule, or always let it through", AnswerLabels.addOrLetThrough(GrammaticalNumber.One))
        assertEquals(
            "Add them to a rule, or always let them through",
            AnswerLabels.addOrLetThrough(GrammaticalNumber.Many),
        )
    }

    @Test
    fun `choosing a rule names the chat`() {
        assertEquals("Add Boss to…", AnswerLabels.choosing(ConversationName("Boss")))
    }
}
