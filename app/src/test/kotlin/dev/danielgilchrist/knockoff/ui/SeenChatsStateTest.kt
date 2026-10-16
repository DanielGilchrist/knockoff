package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.chat.SeenChatsLoad
import dev.danielgilchrist.knockoff.domain.chat.SeenChats
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SeenChatsStateTest {
    @Test
    fun `loaded chats pass through`() {
        val seen = SeenChats.NONE.seen(COLLEAGUE_CHAT)

        assertEquals(SeenChatsState.Loaded(seen.chats), SeenChatsState.of(SeenChatsLoad.Loaded(seen)))
    }

    @Test
    fun `a corrupt or unreadable file is a failure with its detail`() {
        assertEquals(SeenChatsState.Failed("bad"), SeenChatsState.of(SeenChatsLoad.Corrupt("bad")))
        assertEquals(SeenChatsState.Failed("denied"), SeenChatsState.of(SeenChatsLoad.ReadFailed("denied")))
    }
}
