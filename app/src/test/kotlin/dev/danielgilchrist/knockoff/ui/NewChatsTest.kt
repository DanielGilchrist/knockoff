package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.chat.ChatDecisionsLoad
import dev.danielgilchrist.knockoff.chat.SeenChatsLoad
import dev.danielgilchrist.knockoff.domain.chat.ChatDecisions
import dev.danielgilchrist.knockoff.domain.chat.SeenChats
import dev.danielgilchrist.knockoff.domain.settings.Settings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NewChatsTest {
    private val ready = SettingsState.Ready(Settings.DEFAULT, SettingsOrigin.Saved)
    private val seen = SeenChatsLoad.Loaded(SeenChats.NONE.seen(COLLEAGUE_CHAT))
    private val decisions = ChatDecisionsLoad.Loaded(ChatDecisions.NONE)

    @Test
    fun `chats seen but in no rule are new`() {
        assertEquals(NewChats.Ready(listOf(COLLEAGUE_CHAT)), NewChats.of(ready, seen, decisions))
    }

    @Test
    fun `chats let through are not new`() {
        val letThrough = ChatDecisionsLoad.Loaded(ChatDecisions.NONE.lettingThrough(COLLEAGUE_CHAT.key))

        assertEquals(NewChats.Ready(emptyList()), NewChats.of(ready, seen, letThrough))
    }

    @Test
    fun `nothing is new until settings load`() {
        assertEquals(NewChats.Loading, NewChats.of(SettingsState.Loading, seen, decisions))
        assertEquals(NewChats.Loading, NewChats.of(SettingsState.Failed("x"), seen, decisions))
    }

    @Test
    fun `unreadable seen chats or decisions fail`() {
        assertEquals(NewChats.Failed("x"), NewChats.of(ready, SeenChatsLoad.ReadFailed("x"), decisions))
        assertEquals(NewChats.Failed("x"), NewChats.of(ready, SeenChatsLoad.Corrupt("x"), decisions))
        assertEquals(NewChats.Failed("x"), NewChats.of(ready, seen, ChatDecisionsLoad.ReadFailed("x")))
    }

    @Test
    fun `corrupt decisions count as none so chats stay visible`() {
        assertEquals(NewChats.Ready(listOf(COLLEAGUE_CHAT)), NewChats.of(ready, seen, ChatDecisionsLoad.Corrupt("x")))
    }
}
