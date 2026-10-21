package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.ui.editor.BreakDraft
import dev.danielgilchrist.knockoff.ui.editor.RuleDraft
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DescribeTest {
    @Test
    fun `an app's effect follows the rule's policy and hours, and rules without names are this rule`() {
        val draft = RuleDraft.NEW.copy(breaks = listOf(BreakDraft.LUNCH))

        val whatsapp = HeldPhrase.of("WhatsApp", GrammaticalNumber.One)

        assertEquals("WhatsApp only reaches you on weekdays 08:00–17:00, except Lunch.", draft.appEffect(whatsapp))
        assertEquals("", draft.copy(windows = emptyList()).appEffect(whatsapp))
        assertEquals("this rule", ruleCalled(" "))
        assertEquals("Work", ruleCalled("Work "))
        assertEquals("Work's hours", hoursOf(" Work"))
        assertEquals("Errands' hours", hoursOf("Errands"))
        assertEquals("BUS' hours", hoursOf("BUS"))
        assertEquals("this rule's hours", hoursOf(""))
        assertEquals("Gmail's", possessive("Gmail"))
        assertEquals("Messages'", possessive("Messages"))
        assertEquals("WHATSAPPS'", possessive("WHATSAPPS"))
    }

    @Test
    fun `nouns take a or an`() {
        assertEquals("a chat", withArticle("chat"))
        assertEquals("an account", withArticle("account"))
        assertEquals("an account or chat", withArticle("account or chat"))
        assertEquals("a group", withArticle("group"))
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
