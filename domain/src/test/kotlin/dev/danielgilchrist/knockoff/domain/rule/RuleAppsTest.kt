package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.SLACK
import dev.danielgilchrist.knockoff.domain.WHATSAPP
import dev.danielgilchrist.knockoff.domain.WORK_ADDRESS
import dev.danielgilchrist.knockoff.domain.chat.SeenChats
import dev.danielgilchrist.knockoff.domain.gmailAccount
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.ConversationId
import dev.danielgilchrist.knockoff.domain.notification.ConversationName
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.ruleApps
import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.shouldBeInstanceOf
import dev.danielgilchrist.knockoff.domain.textPattern
import org.junit.jupiter.api.Test

class RuleAppsTest {
    private val pattern = Target.ChatNamePattern(WHATSAPP, textPattern("^Work"))
    private val friend = COLLEAGUE_CHAT.copy(id = ConversationId("2@lid"), name = ConversationName("Work chat"))
    private val account = WORK_ADDRESS.gmailAccount()

    @Test
    fun `an app target holds the whole app and absorbs its narrower picks`() {
        val apps = ruleApps(setOf(Target.App(WHATSAPP), COLLEAGUE_CHAT, pattern), setOf(WHATSAPP))

        apps.scopes shouldBe mapOf(WHATSAPP to AppScope.Everything)
        apps.targets shouldBe setOf(Target.App(WHATSAPP))
        apps.asks(WHATSAPP) shouldBe false
    }

    @Test
    fun `narrower picks are grouped under their own app`() {
        val apps = ruleApps(setOf(account, COLLEAGUE_CHAT, pattern), setOf(WHATSAPP, SLACK))

        apps.scopes.keys shouldBe setOf(account.packageName, WHATSAPP, SLACK)
        apps.targets shouldBe setOf(account, COLLEAGUE_CHAT, pattern)
        apps.asking shouldBe setOf(WHATSAPP, SLACK)
        apps.asks(account.packageName) shouldBe false
        apps.asks(PackageName("org.signal")) shouldBe false
    }

    @Test
    fun `adding a chat narrows a new app, joins a narrowed one and leaves a whole one`() {
        val other = COLLEAGUE_CHAT.copy(packageName = SLACK)

        RuleApps.NONE.withChat(COLLEAGUE_CHAT).targets shouldBe setOf(COLLEAGUE_CHAT)
        ruleApps(setOf(pattern), emptySet()).withChat(COLLEAGUE_CHAT).targets shouldBe
            setOf(pattern, COLLEAGUE_CHAT)
        ruleApps(setOf(Target.App(SLACK)), emptySet()).withChat(other).targets shouldBe
            setOf(Target.App(SLACK))
    }

    @Test
    fun `an app's picks are empty when it's held whole or missing`() {
        val apps = ruleApps(setOf(COLLEAGUE_CHAT, account, Target.App(SLACK)), setOf(WHATSAPP))

        apps.chatsOf(WHATSAPP).keys shouldBe setOf(COLLEAGUE_CHAT.id)
        apps.accountsOf(account.packageName) shouldBe mapOf(account.group to account.label)
        apps.chatsOf(SLACK) shouldBe emptyMap()
        apps.accountsOf(SLACK) shouldBe emptyMap()
        apps.chatsOf(PackageName("org.signal")) shouldBe emptyMap()
        apps.accountsOf(PackageName("org.signal")) shouldBe emptyMap()
    }

    @Test
    fun `rebasing keeps the edit, chats added since it opened and apps gained since`() {
        val opened = ruleApps(setOf(COLLEAGUE_CHAT, account), emptySet())
        val current =
            opened
                .withChat(friend)
                .withChat(COLLEAGUE_CHAT.copy(packageName = SLACK))
        val edited = ruleApps(setOf(pattern), emptySet())

        edited.rebased(opened, current).targets shouldBe
            setOf(pattern, friend, COLLEAGUE_CHAT.copy(packageName = SLACK))
    }

    @Test
    fun `rebasing leaves a whole app whole and an app the edit kept wins over the stored one`() {
        val opened = ruleApps(setOf(COLLEAGUE_CHAT), emptySet())
        val current = opened.withChat(friend)
        val whole = ruleApps(setOf(Target.App(WHATSAPP)), emptySet())

        whole.rebased(opened, current).targets shouldBe setOf(Target.App(WHATSAPP))
        whole.rebased(RuleApps.NONE, current).targets shouldBe setOf(Target.App(WHATSAPP))
    }

    @Test
    fun `rebasing takes no chats from an app that was missing or whole`() {
        val edited = ruleApps(setOf(pattern), emptySet())
        val whole = ruleApps(setOf(Target.App(WHATSAPP)), emptySet())

        edited.rebased(RuleApps.NONE, whole) shouldBe edited
        edited.rebased(whole, RuleApps.NONE) shouldBe edited
    }

    @Test
    fun `expanding refreshes picked chats' channels and adds seen chats a pattern names`() {
        val moved = COLLEAGUE_CHAT.copy(channel = ChannelId("individual_chat_defaults_5"))
        val seen =
            SeenChats.NONE
                .seen(moved)
                .seen(friend)
                .seen(friend.copy(packageName = SLACK))
        val apps = ruleApps(setOf(COLLEAGUE_CHAT, pattern, Target.App(SLACK)), emptySet())

        apps.expanded(seen).targets shouldBe setOf(moved, friend, pattern, Target.App(SLACK))
    }

    @Test
    fun `expanding only takes seen chats from the same app`() {
        val elsewhere = friend.copy(packageName = SLACK)
        val samePicked = COLLEAGUE_CHAT.copy(packageName = SLACK, channel = ChannelId("elsewhere"))
        val apps = ruleApps(setOf(COLLEAGUE_CHAT, pattern), emptySet())

        apps.expanded(SeenChats.NONE.seen(elsewhere).seen(samePicked)).targets shouldBe setOf(COLLEAGUE_CHAT, pattern)
    }

    @Test
    fun `an app narrowed to nothing is refused`() {
        AppScope.Narrowed.parse(emptyMap(), emptyList(), emptySet(), NewChatsAsking.DontAsk) shouldBe
            NarrowedParse.NothingChosen
        AppScope.Narrowed
            .parse(emptyMap(), emptyList(), emptySet(), NewChatsAsking.Ask)
            .shouldBeInstanceOf<NarrowedParse.Valid>()
        AppScope.Narrowed
            .parse(mapOf(account.group to account.label), emptyList(), emptySet(), NewChatsAsking.DontAsk)
            .shouldBeInstanceOf<NarrowedParse.Valid>()
        AppScope.Narrowed
            .parse(emptyMap(), listOf(ChatPick.of(COLLEAGUE_CHAT)), emptySet(), NewChatsAsking.DontAsk)
            .shouldBeInstanceOf<NarrowedParse.Valid>()
        AppScope.Narrowed
            .parse(emptyMap(), emptyList(), setOf(PatternPick.of(pattern)), NewChatsAsking.DontAsk)
            .shouldBeInstanceOf<NarrowedParse.Valid>()
    }

    @Test
    fun `picks become targets in their app`() {
        ChatPick.of(COLLEAGUE_CHAT).inApp(WHATSAPP) shouldBe COLLEAGUE_CHAT
        PatternPick.of(pattern).inApp(WHATSAPP) shouldBe pattern
        val channels = Target.ChannelPattern(WHATSAPP, textPattern("^individual"))
        PatternPick.of(channels).inApp(WHATSAPP) shouldBe channels
    }
}
