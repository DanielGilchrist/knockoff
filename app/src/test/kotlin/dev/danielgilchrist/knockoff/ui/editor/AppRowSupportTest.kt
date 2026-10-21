package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.rule.ChatPick
import dev.danielgilchrist.knockoff.ui.GroupNoun
import dev.danielgilchrist.knockoff.ui.components.SupportingTone
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppRowSupportTest {
    private val whatsapp = COLLEAGUE_CHAT.packageName
    private val chats =
        AppChoices(AppKind.Chats, AccountChoices(GroupNoun.Categories, emptyMap(), emptySet(), GroupNaming.AllNamed))
    private val narrowed = AppDraft.EVERYTHING.copy(mode = AppMode.Narrowed)
    private val picked = narrowed.copy(chats = mapOf(COLLEAGUE_CHAT.id to ChatPick.of(COLLEAGUE_CHAT)))

    @Test
    fun `a missing app says so plainly even with nothing picked`() {
        val support = AppRowSupport.of(whatsapp, true, narrowed, chats)

        assertEquals(AppRowSupport.Missing, support)
        assertEquals("Not installed", support.text)
        assertEquals(SupportingTone.Plain, support.tone)
    }

    @Test
    fun `an installed app with nothing picked is flagged as a problem`() {
        val support = AppRowSupport.of(whatsapp, false, narrowed, chats)

        assertEquals(AppRowSupport.Unpicked("No chats picked"), support)
        assertEquals("No chats picked", support.text)
        assertEquals(SupportingTone.Problem, support.tone)
    }

    @Test
    fun `otherwise the row summarises the app plainly`() {
        assertEquals(
            AppRowSupport.Summary("All notifications"),
            AppRowSupport.of(whatsapp, false, AppDraft.EVERYTHING, chats),
        )

        val support = AppRowSupport.of(whatsapp, false, picked, chats)

        assertEquals(AppRowSupport.Summary("Colleague"), support)
        assertEquals("Colleague", support.text)
        assertEquals(SupportingTone.Plain, support.tone)
    }
}
