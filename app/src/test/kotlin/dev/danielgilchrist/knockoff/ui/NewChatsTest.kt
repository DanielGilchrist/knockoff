package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.chat.ChatDecisionsLoad
import dev.danielgilchrist.knockoff.chat.SeenChatsLoad
import dev.danielgilchrist.knockoff.domain.chat.ChatDecisions
import dev.danielgilchrist.knockoff.domain.chat.SeenChats
import dev.danielgilchrist.knockoff.domain.dayoff.HolidayCalendar
import dev.danielgilchrist.knockoff.domain.group.KnownGroups
import dev.danielgilchrist.knockoff.domain.rule.RuleApps
import dev.danielgilchrist.knockoff.domain.rule.RuleSetParse
import dev.danielgilchrist.knockoff.domain.settings.Settings
import dev.danielgilchrist.knockoff.group.KnownGroupsLoad
import dev.danielgilchrist.knockoff.ruleApps
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class NewChatsTest {
    private val work =
        Settings.DEFAULT.rules.rules
            .single()
            .let { it.copy(apps = ruleApps(it.targets, setOf(COLLEAGUE_CHAT.packageName))) }
    private val ready =
        SettingsState.Ready(
            Settings(
                assertInstanceOf(RuleSetParse.Valid::class.java, Settings.DEFAULT.rules.withRules(listOf(work))).rules,
                Settings.DEFAULT.theme,
                HolidayCalendar.None,
            ),
            SettingsOrigin.Saved,
        )
    private val seen = SeenChatsLoad.Loaded(SeenChats.NONE.seen(COLLEAGUE_CHAT))
    private val decisions = ChatDecisionsLoad.Loaded(ChatDecisions.NONE)
    private val groups = KnownGroupsLoad.Loaded(KnownGroups.NONE)

    @Test
    fun `chats seen but in no rule are new`() {
        assertEquals(NewChats.Ready(listOf(COLLEAGUE_CHAT)), NewChats.of(ready, seen, decisions, groups))
    }

    @Test
    fun `chats are only new when a rule asks about their app`() {
        val notAsking = SettingsState.Ready(Settings.DEFAULT, SettingsOrigin.Saved)

        assertEquals(NewChats.Ready(emptyList()), NewChats.of(notAsking, seen, decisions, groups))
    }

    @Test
    fun `chats let through are not new`() {
        val letThrough = ChatDecisionsLoad.Loaded(ChatDecisions.NONE.lettingThrough(COLLEAGUE_CHAT.key))

        assertEquals(NewChats.Ready(emptyList()), NewChats.of(ready, seen, letThrough, groups))
    }

    @Test
    fun `nothing is new until settings load`() {
        assertEquals(NewChats.Loading, NewChats.of(SettingsState.Loading, seen, decisions, groups))
        assertEquals(NewChats.Loading, NewChats.of(SettingsState.Failed("x"), seen, decisions, groups))
    }

    @Test
    fun `unreadable seen chats or decisions fail`() {
        assertEquals(
            NewChats.Failed("x"),
            NewChats.of(ready, SeenChatsLoad.ReadFailed("x"), decisions, groups),
        )
        assertEquals(NewChats.Failed("x"), NewChats.of(ready, SeenChatsLoad.Corrupt("x"), decisions, groups))
        assertEquals(
            NewChats.Failed("x"),
            NewChats.of(ready, seen, ChatDecisionsLoad.ReadFailed("x"), groups),
        )
    }

    @Test
    fun `unreadable channel groups fail rather than show chats a group already holds`() {
        assertEquals(NewChats.Failed("x"), NewChats.of(ready, seen, decisions, KnownGroupsLoad.Corrupt("x")))
        assertEquals(NewChats.Failed("x"), NewChats.of(ready, seen, decisions, KnownGroupsLoad.ReadFailed("x")))
    }

    @Test
    fun `corrupt decisions count as none so chats stay visible`() {
        assertEquals(
            NewChats.Ready(listOf(COLLEAGUE_CHAT)),
            NewChats.of(ready, seen, ChatDecisionsLoad.Corrupt("x"), groups),
        )
    }
}
