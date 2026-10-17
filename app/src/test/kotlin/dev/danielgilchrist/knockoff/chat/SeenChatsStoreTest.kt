package dev.danielgilchrist.knockoff.chat

import dev.danielgilchrist.knockoff.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.chat.SeenChats
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.ConversationId
import dev.danielgilchrist.knockoff.domain.notification.ConversationName
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File

class SeenChatsStoreTest {
    @TempDir
    lateinit var directory: File

    @Test
    fun `a missing file has seen no chats`() =
        runTest {
            assertEquals(SeenChatsLoad.Loaded(SeenChats.NONE), store().loads.first())
        }

    @Test
    fun `a recorded chat loads back`() =
        runTest {
            val store = store()

            assertEquals(ChatRecord.Recorded, store.record(COLLEAGUE_CHAT))
            assertEquals(SeenChatsLoad.Loaded(SeenChats.NONE.seen(COLLEAGUE_CHAT)), store.loads.first())
        }

    @Test
    fun `recording the latest chat again changes nothing`() =
        runTest {
            val store = store()
            store.record(COLLEAGUE_CHAT)

            assertEquals(ChatRecord.Unchanged, store.record(COLLEAGUE_CHAT))
        }

    @Test
    fun `a known chat seen again after another moves first`() =
        runTest {
            val store = store()
            val friend = COLLEAGUE_CHAT.copy(id = ConversationId("1000000000002@lid"))
            store.record(COLLEAGUE_CHAT)
            store.record(friend)

            assertEquals(ChatRecord.Recorded, store.record(COLLEAGUE_CHAT))
        }

    @Test
    fun `the latest chat with a new name or channel is recorded again`() =
        runTest {
            val store = store()
            store.record(COLLEAGUE_CHAT)
            val renamed =
                COLLEAGUE_CHAT.copy(
                    channel = ChannelId("individual_chat_defaults_5"),
                    name = ConversationName("Boss"),
                )

            assertEquals(ChatRecord.Recorded, store.record(renamed))
            assertEquals(SeenChatsLoad.Loaded(SeenChats.NONE.seen(renamed)), store.loads.first())
        }

    @Test
    fun `a running collector sees each recorded chat`() =
        runTest {
            val store = store()
            val loads = Channel<SeenChatsLoad>(Channel.UNLIMITED)
            backgroundScope.launch { store.loads.collect(loads::send) }
            assertEquals(SeenChatsLoad.Loaded(SeenChats.NONE), loads.receive())

            store.record(COLLEAGUE_CHAT)

            assertEquals(SeenChatsLoad.Loaded(SeenChats.NONE.seen(COLLEAGUE_CHAT)), loads.receive())
        }

    @ParameterizedTest
    @ValueSource(strings = ["not json", """{"version": 2, "chats": []}"""])
    fun `an unusable document is replaced by the new chat`(text: String) =
        runTest {
            file().writeText(text)
            val store = store()

            assertInstanceOf(SeenChatsLoad.Corrupt::class.java, store.loads.first())
            assertEquals(ChatRecord.Recorded, store.record(COLLEAGUE_CHAT))
            assertEquals(SeenChatsLoad.Loaded(SeenChats.NONE.seen(COLLEAGUE_CHAT)), store.loads.first())
        }

    @Test
    fun `recording onto a file with chats named by their id writes it without them`() =
        runTest {
            val byId = """{"app": "com.whatsapp", "channel": "chats", "id": "2@lid", "name": "2@lid"}"""
            file().writeText("""{"version": 1, "chats": [$byId]}""")
            val store = store()

            store.record(COLLEAGUE_CHAT)

            assertEquals(SeenChats.NONE.seen(COLLEAGUE_CHAT).toJson(), file().readText())
        }

    @Test
    fun `undecodable bytes are replaced by the new chat`() =
        runTest {
            file().writeBytes(byteArrayOf(0xC3.toByte()))
            val store = store()

            assertInstanceOf(SeenChatsLoad.Corrupt::class.java, store.loads.first())
            assertEquals(ChatRecord.Recorded, store.record(COLLEAGUE_CHAT))
            assertEquals(SeenChatsLoad.Loaded(SeenChats.NONE.seen(COLLEAGUE_CHAT)), store.loads.first())
        }

    @Test
    fun `an unreadable file is left alone`() =
        runTest {
            val store = store()
            store.record(COLLEAGUE_CHAT)
            file().setReadable(false)

            assertInstanceOf(SeenChatsLoad.ReadFailed::class.java, store.loads.first())
            assertInstanceOf(
                ChatRecord.Failed::class.java,
                store.record(COLLEAGUE_CHAT.copy(id = ConversationId("1@lid"))),
            )
            file().setReadable(true)
            assertEquals(SeenChatsLoad.Loaded(SeenChats.NONE.seen(COLLEAGUE_CHAT)), store.loads.first())
        }

    @Test
    fun `a chat that can't be written fails`() =
        runTest {
            directory.setWritable(false)

            val record = store().record(COLLEAGUE_CHAT)

            directory.setWritable(true)
            assertInstanceOf(ChatRecord.Failed::class.java, record)
        }

    private fun store(): SeenChatsStore = SeenChatsStore(file())

    private fun file(): File = File(directory, "seen-chats.json")
}
