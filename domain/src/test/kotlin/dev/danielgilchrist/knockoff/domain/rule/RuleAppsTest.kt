package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.SLACK
import dev.danielgilchrist.knockoff.domain.WHATSAPP
import dev.danielgilchrist.knockoff.domain.WORK_ADDRESS
import dev.danielgilchrist.knockoff.domain.chat.SeenChats
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.ConversationId
import dev.danielgilchrist.knockoff.domain.notification.ConversationName
import dev.danielgilchrist.knockoff.domain.notification.PackageName
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
        val apps = RuleApps.fromTargets(setOf(Target.App(WHATSAPP), COLLEAGUE_CHAT, pattern), setOf(WHATSAPP))

        apps.scopes shouldBe mapOf(WHATSAPP to AppScope.Everything)
        apps.targets shouldBe setOf(Target.App(WHATSAPP))
        apps.asks(WHATSAPP) shouldBe false
    }

    @Test
    fun `narrower picks are grouped under their own app`() {
        val apps = RuleApps.fromTargets(setOf(account, COLLEAGUE_CHAT, pattern), setOf(WHATSAPP, SLACK))

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
        RuleApps.fromTargets(setOf(pattern), emptySet()).withChat(COLLEAGUE_CHAT).targets shouldBe
            setOf(pattern, COLLEAGUE_CHAT)
        RuleApps.fromTargets(setOf(Target.App(SLACK)), emptySet()).withChat(other).targets shouldBe
            setOf(Target.App(SLACK))
    }

    @Test
    fun `expanding refreshes picked chats' channels and adds seen chats a pattern names`() {
        val moved = COLLEAGUE_CHAT.copy(channel = ChannelId("individual_chat_defaults_5"))
        val seen =
            SeenChats.NONE
                .seen(moved)
                .seen(friend)
                .seen(friend.copy(packageName = SLACK))
        val apps = RuleApps.fromTargets(setOf(COLLEAGUE_CHAT, pattern, Target.App(SLACK)), emptySet())

        apps.expanded(seen).targets shouldBe setOf(moved, friend, pattern, Target.App(SLACK))
    }

    @Test
    fun `expanding only takes seen chats from the same app`() {
        val elsewhere = friend.copy(packageName = SLACK)
        val samePicked = COLLEAGUE_CHAT.copy(packageName = SLACK, channel = ChannelId("elsewhere"))
        val apps = RuleApps.fromTargets(setOf(COLLEAGUE_CHAT, pattern), emptySet())

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
