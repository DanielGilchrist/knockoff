package dev.danielgilchrist.knockoff.ask

import dev.danielgilchrist.knockoff.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.chat.ChatDecisionsLoad
import dev.danielgilchrist.knockoff.chat.SeenChatsLoad
import dev.danielgilchrist.knockoff.domain.chat.ChatDecisions
import dev.danielgilchrist.knockoff.domain.chat.ChatQuestion
import dev.danielgilchrist.knockoff.domain.chat.SeenChats
import dev.danielgilchrist.knockoff.domain.dayoff.HolidayCalendar
import dev.danielgilchrist.knockoff.domain.group.KnownGroups
import dev.danielgilchrist.knockoff.domain.notification.ConversationId
import dev.danielgilchrist.knockoff.domain.notification.ConversationName
import dev.danielgilchrist.knockoff.domain.rule.RuleApps
import dev.danielgilchrist.knockoff.domain.rule.RuleSetParse
import dev.danielgilchrist.knockoff.domain.settings.Settings
import dev.danielgilchrist.knockoff.domain.settings.SettingsDecode
import dev.danielgilchrist.knockoff.group.KnownGroupsLoad
import dev.danielgilchrist.knockoff.ruleApps
import dev.danielgilchrist.knockoff.settings.SettingsLoad
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class ChatPromptTest {
    private val work =
        Settings.DEFAULT.rules.rules
            .single()
            .let { it.copy(apps = ruleApps(it.targets, setOf(COLLEAGUE_CHAT.packageName))) }
    private val asking =
        SettingsLoad.Loaded(
            Settings(
                assertInstanceOf(RuleSetParse.Valid::class.java, Settings.DEFAULT.rules.withRules(listOf(work))).rules,
                Settings.DEFAULT.theme,
                HolidayCalendar.None,
            ),
        )
    private val groups = KnownGroupsLoad.Loaded(KnownGroups.NONE)
    private val seen = SeenChatsLoad.Loaded(SeenChats.NONE.seen(COLLEAGUE_CHAT))
    private val question = ChatQuestion(COLLEAGUE_CHAT, work.name)

    @Test
    fun `loaded inputs ask about undecided chats`() {
        assertEquals(
            ChatPrompt.Ready(listOf(question)),
            ChatPrompt.of(asking, seen, ChatDecisionsLoad.Loaded(ChatDecisions.NONE), groups),
        )
        assertEquals(
            ChatPrompt.Ready(emptyList()),
            ChatPrompt.of(SettingsLoad.Empty, seen, ChatDecisionsLoad.Loaded(ChatDecisions.NONE), groups),
        )
    }

    @Test
    fun `questions are ordered by chat name so a new message doesn't reorder them`() {
        val aaron = COLLEAGUE_CHAT.copy(id = ConversationId("2@lid"), name = ConversationName("aaron"))
        val newestFirst = SeenChatsLoad.Loaded(SeenChats.NONE.seen(COLLEAGUE_CHAT).seen(aaron))
        val other = SeenChatsLoad.Loaded(SeenChats.NONE.seen(aaron).seen(COLLEAGUE_CHAT))
        val none = ChatDecisionsLoad.Loaded(ChatDecisions.NONE)
        val expected = ChatPrompt.Ready(listOf(ChatQuestion(aaron, work.name), question))

        assertEquals(expected, ChatPrompt.of(asking, newestFirst, none, groups))
        assertEquals(expected, ChatPrompt.of(asking, other, none, groups))
    }

    @Test
    fun `corrupt decisions ask as if none were made`() {
        assertEquals(
            ChatPrompt.Ready(listOf(question)),
            ChatPrompt.of(asking, seen, ChatDecisionsLoad.Corrupt("x"), groups),
        )
    }

    @Test
    fun `anything unreadable leaves the prompt unknown`() {
        val none = ChatDecisionsLoad.Loaded(ChatDecisions.NONE)

        assertEquals(ChatPrompt.Unknown, ChatPrompt.of(SettingsLoad.ReadFailed("x"), seen, none, groups))
        assertEquals(
            ChatPrompt.Unknown,
            ChatPrompt.of(SettingsLoad.Corrupt(SettingsDecode.Invalid("x")), seen, none, groups),
        )
        assertEquals(ChatPrompt.Unknown, ChatPrompt.of(asking, SeenChatsLoad.Corrupt("x"), none, groups))
        assertEquals(ChatPrompt.Unknown, ChatPrompt.of(asking, SeenChatsLoad.ReadFailed("x"), none, groups))
        assertEquals(
            ChatPrompt.Unknown,
            ChatPrompt.of(asking, seen, ChatDecisionsLoad.ReadFailed("x"), groups),
        )
    }

    @Test
    fun `unreadable channel groups leave the prompt unknown`() {
        val none = ChatDecisionsLoad.Loaded(ChatDecisions.NONE)

        assertEquals(ChatPrompt.Unknown, ChatPrompt.of(asking, seen, none, KnownGroupsLoad.Corrupt("x")))
        assertEquals(ChatPrompt.Unknown, ChatPrompt.of(asking, seen, none, KnownGroupsLoad.ReadFailed("x")))
    }
}
