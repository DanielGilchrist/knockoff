package dev.danielgilchrist.knockoff.chat

import dev.danielgilchrist.knockoff.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.chat.ChatDecisions
import dev.danielgilchrist.knockoff.domain.notification.ConversationId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ChatDecisionsStoreTest {
    @TempDir
    lateinit var directory: File

    private val decided = ChatDecisions.NONE.lettingThrough(COLLEAGUE_CHAT.key)

    @Test
    fun `a missing file has no decisions`() =
        runTest {
            assertEquals(ChatDecisionsLoad.Loaded(ChatDecisions.NONE), store().loads.first())
        }

    @Test
    fun `letting a chat through is saved`() =
        runTest {
            val store = store()

            assertEquals(ChatDecisionSave.Saved, store.letThrough(COLLEAGUE_CHAT.key))
            assertEquals(ChatDecisionsLoad.Loaded(decided), store.loads.first())
        }

    @Test
    fun `later decisions keep earlier ones across stores`() =
        runTest {
            val other = COLLEAGUE_CHAT.copy(id = ConversationId("2@lid")).key
            store().letThrough(COLLEAGUE_CHAT.key)

            assertEquals(ChatDecisionSave.Saved, store().letThrough(other))
            assertEquals(ChatDecisionsLoad.Loaded(decided.lettingThrough(other)), store().loads.first())
        }

    @Test
    fun `asked chats are saved alongside those let through`() =
        runTest {
            val store = store()
            store.letThrough(COLLEAGUE_CHAT.key)

            assertEquals(ChatDecisionSave.Saved, store.askedAbout(setOf(COLLEAGUE_CHAT.key)))
            assertEquals(ChatDecisionsLoad.Loaded(decided.askedAbout(setOf(COLLEAGUE_CHAT.key))), store().loads.first())
        }

    @Test
    fun `a corrupt file is replaced`() =
        runTest {
            file().writeText("not json")
            val store = store()

            assertInstanceOf(ChatDecisionsLoad.Corrupt::class.java, store.loads.first())
            assertEquals(ChatDecisionSave.Saved, store.letThrough(COLLEAGUE_CHAT.key))
            assertEquals(ChatDecisionsLoad.Loaded(decided), store.loads.first())
        }

    @Test
    fun `an unreadable file is left alone`() =
        runTest {
            file().writeText(ChatDecisions.NONE.toJson())
            file().setReadable(false)

            assertInstanceOf(ChatDecisionsLoad.ReadFailed::class.java, store().loads.first())
            assertInstanceOf(ChatDecisionSave.Failed::class.java, store().letThrough(COLLEAGUE_CHAT.key))
            file().setReadable(true)
        }

    @Test
    fun `a decision that can't be written fails`() =
        runTest {
            directory.setWritable(false)

            val save = store().letThrough(COLLEAGUE_CHAT.key)

            directory.setWritable(true)
            assertInstanceOf(ChatDecisionSave.Failed::class.java, save)
        }

    private fun store(): ChatDecisionsStore = ChatDecisionsStore(file())

    private fun file(): File = File(directory, "chat-decisions.json")
}
