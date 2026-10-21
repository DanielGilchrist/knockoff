package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.ui.editor.BreakDraft
import dev.danielgilchrist.knockoff.ui.editor.RuleDraft
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DescribeTest {
    @Test
    fun `an app's effect follows the rule's policy and hours, and rules without names are this rule`() {
        val draft = RuleDraft.NEW.copy(breaks = listOf(BreakDraft.LUNCH))

        assertEquals("Reaches you only on weekdays 08:00–17:00, except Lunch.", draft.appEffect())
        assertEquals("Held on weekdays 08:00–17:00, except Lunch.", draft.copy(policy = Policy.HoldDuring).appEffect())
        assertEquals("", draft.copy(windows = emptyList()).appEffect())
        assertEquals("this rule", ruleCalled(" "))
        assertEquals("Work", ruleCalled("Work "))
        assertEquals("Work's hours", hoursOf(" Work"))
        assertEquals("Errands' hours", hoursOf("Errands"))
        assertEquals("BUS' hours", hoursOf("BUS"))
        assertEquals("this rule's hours", hoursOf(""))
    }

    @Test
    fun `counts read one or many`() {
        assertEquals("1 chat", counted(1, "chat", "chats"))
        assertEquals("0 chats", counted(0, "chat", "chats"))
        assertEquals("3 chats", counted(3, "chat", "chats"))
    }

    @Test
    fun `seen chats describe their loading and failure, or what was loaded`() {
        assertEquals("Loading chats…", SeenChatsState.Loading.describe { "x" })
        assertEquals("Couldn't read the chats seen so far: gone", SeenChatsState.Failed("gone").describe { "x" })
        assertEquals("1", SeenChatsState.Loaded(listOf(COLLEAGUE_CHAT)).describe { it.size.toString() })
    }
}
