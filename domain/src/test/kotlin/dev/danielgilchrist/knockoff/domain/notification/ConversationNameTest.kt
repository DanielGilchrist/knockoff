package dev.danielgilchrist.knockoff.domain.notification

import dev.danielgilchrist.knockoff.domain.COLLEAGUE
import dev.danielgilchrist.knockoff.domain.shouldBe
import org.junit.jupiter.api.Test

class ConversationNameTest {
    @Test
    fun `the first candidate wins`() {
        ConversationName.chosen(listOf("Boss", "Work chat", "Boss (2 messages)"), COLLEAGUE) shouldBe
            ConversationName("Boss")
    }

    @Test
    fun `blank candidates are skipped and the chosen one is trimmed`() {
        ConversationName.chosen(listOf("", "  ", " Work chat "), COLLEAGUE) shouldBe ConversationName("Work chat")
    }

    @Test
    fun `without a usable candidate the conversation id is the name`() {
        ConversationName.chosen(emptyList(), COLLEAGUE) shouldBe ConversationName(COLLEAGUE.value)
        ConversationName.chosen(listOf(" "), COLLEAGUE) shouldBe ConversationName(COLLEAGUE.value)
    }
}
