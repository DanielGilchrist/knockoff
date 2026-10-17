package dev.danielgilchrist.knockoff.ask

import dev.danielgilchrist.knockoff.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.chat.ChatDecisionsLoad
import dev.danielgilchrist.knockoff.chat.SeenChatsLoad
import dev.danielgilchrist.knockoff.domain.chat.ChatDecisions
import dev.danielgilchrist.knockoff.domain.chat.ChatQuestion
import dev.danielgilchrist.knockoff.domain.chat.SeenChats
import dev.danielgilchrist.knockoff.domain.notification.ConversationId
import dev.danielgilchrist.knockoff.domain.notification.ConversationName
import dev.danielgilchrist.knockoff.domain.rule.RuleSetParse
import dev.danielgilchrist.knockoff.domain.settings.Settings
import dev.danielgilchrist.knockoff.domain.settings.SettingsDecode
import dev.danielgilchrist.knockoff.settings.SettingsLoad
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class ChatPromptTest {
    private val work =
        Settings.DEFAULT.rules.rules
            .single()
            .copy(asking = setOf(COLLEAGUE_CHAT.packageName))
    private val asking =
        SettingsLoad.Loaded(
            Settings(
                assertInstanceOf(RuleSetParse.Valid::class.java, Settings.DEFAULT.rules.withRules(listOf(work))).rules,
                Settings.DEFAULT.theme,
            ),
        )
    private val seen = SeenChatsLoad.Loaded(SeenChats.NONE.seen(COLLEAGUE_CHAT))
    private val question = ChatQuestion(COLLEAGUE_CHAT, listOf(work.name))

    @Test
    fun `loaded inputs ask about undecided chats`() {
        assertEquals(
            ChatPrompt.Ready(listOf(question)),
            ChatPrompt.of(asking, seen, ChatDecisionsLoad.Loaded(ChatDecisions.NONE)),
        )
        assertEquals(
            ChatPrompt.Ready(emptyList()),
            ChatPrompt.of(SettingsLoad.Empty, seen, ChatDecisionsLoad.Loaded(ChatDecisions.NONE)),
        )
    }

    @Test
    fun `questions are ordered by chat name so a new message doesn't reorder them`() {
        val aaron = COLLEAGUE_CHAT.copy(id = ConversationId("2@lid"), name = ConversationName("aaron"))
        val newestFirst = SeenChatsLoad.Loaded(SeenChats.NONE.seen(COLLEAGUE_CHAT).seen(aaron))
        val other = SeenChatsLoad.Loaded(SeenChats.NONE.seen(aaron).seen(COLLEAGUE_CHAT))
        val none = ChatDecisionsLoad.Loaded(ChatDecisions.NONE)
        val expected = ChatPrompt.Ready(listOf(ChatQuestion(aaron, listOf(work.name)), question))

        assertEquals(expected, ChatPrompt.of(asking, newestFirst, none))
        assertEquals(expected, ChatPrompt.of(asking, other, none))
    }

    @Test
    fun `corrupt decisions ask as if none were made`() {
        assertEquals(ChatPrompt.Ready(listOf(question)), ChatPrompt.of(asking, seen, ChatDecisionsLoad.Corrupt("x")))
    }

    @Test
    fun `anything unreadable leaves the prompt unknown`() {
        val none = ChatDecisionsLoad.Loaded(ChatDecisions.NONE)

        assertEquals(ChatPrompt.Unknown, ChatPrompt.of(SettingsLoad.ReadFailed("x"), seen, none))
        assertEquals(ChatPrompt.Unknown, ChatPrompt.of(SettingsLoad.Corrupt(SettingsDecode.Invalid("x")), seen, none))
        assertEquals(ChatPrompt.Unknown, ChatPrompt.of(asking, SeenChatsLoad.Corrupt("x"), none))
        assertEquals(ChatPrompt.Unknown, ChatPrompt.of(asking, SeenChatsLoad.ReadFailed("x"), none))
        assertEquals(ChatPrompt.Unknown, ChatPrompt.of(asking, seen, ChatDecisionsLoad.ReadFailed("x")))
    }
}
