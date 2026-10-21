package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId
import dev.danielgilchrist.knockoff.domain.notification.ConversationId
import dev.danielgilchrist.knockoff.domain.notification.ConversationName
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleApps
import dev.danielgilchrist.knockoff.domain.rule.Target
import dev.danielgilchrist.knockoff.domain.rule.TextPattern
import dev.danielgilchrist.knockoff.domain.rule.TextPatternParse
import dev.danielgilchrist.knockoff.domain.settings.Settings
import dev.danielgilchrist.knockoff.groupName
import dev.danielgilchrist.knockoff.ruleApps
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class HeldPhrasesTest {
    private val whatsapp = COLLEAGUE_CHAT.packageName
    private val gmail = PackageName.GMAIL
    private val slack = PackageName("com.Slack")
    private val labels = mapOf(whatsapp to "WhatsApp", gmail to "Gmail", slack to "Slack")
    private val boss = COLLEAGUE_CHAT.copy(id = ConversationId("2@lid"), name = ConversationName("Boss"))
    private val work = account(gmail, "a", "dan@work.example")
    private val home = account(gmail, "b", "dan@home.example")

    @Test
    fun `a whole app is its name, one pick is named and several are counted`() {
        assertEquals(listOf(one("Slack")), phrases(Target.App(slack)))
        assertEquals(listOf(one("dan@work.example in Gmail")), phrases(work))
        assertEquals(listOf(many("2 Gmail accounts")), phrases(work, home))
        assertEquals(listOf(one("Colleague on WhatsApp")), phrases(COLLEAGUE_CHAT))
        assertEquals(listOf(many("2 WhatsApp chats")), phrases(COLLEAGUE_CHAT, boss))
    }

    @Test
    fun `groups without addresses are counted as groups`() {
        val chats = account(slack, "c", "Chats")
        val calls = account(slack, "d", "Calls")

        assertEquals(listOf(one("Chats in Slack")), phrases(chats))
        assertEquals(listOf(many("2 Slack categories")), phrases(chats, calls))
    }

    @Test
    fun `accounts and chats in one app are combined without another and`() {
        val chat = COLLEAGUE_CHAT.copy(packageName = gmail)
        val slackChat = COLLEAGUE_CHAT.copy(packageName = slack)

        assertEquals(listOf(many("1 account plus 1 chat in Gmail")), phrases(work, chat))
        assertEquals(
            listOf(many("2 accounts plus 2 chats in Gmail")),
            phrases(work, home, chat, boss.copy(packageName = gmail)),
        )
        assertEquals(
            listOf(many("1 category plus 1 chat in Slack")),
            phrases(account(slack, "c", "Chats"), slackChat),
        )
        assertEquals(
            listOf(many("2 categories plus 2 chats in Slack")),
            phrases(
                account(slack, "c", "Chats"),
                account(slack, "d", "Calls"),
                slackChat,
                boss.copy(packageName = slack),
            ),
        )
    }

    @Test
    fun `patterns make an app's phrase some of its chats or notifications`() {
        val workChats = Target.ChatNamePattern(whatsapp, pattern("^Work"))

        assertEquals(listOf(some("WhatsApp chats")), phrases(workChats))
        assertEquals(listOf(some("WhatsApp chats")), phrases(COLLEAGUE_CHAT, workChats))
        assertEquals(listOf(some("Slack notifications")), phrases(Target.ChannelPattern(slack, pattern("^dm"))))
        assertEquals(
            listOf(some("WhatsApp notifications")),
            phrases(workChats, Target.ChannelPattern(whatsapp, pattern("^dm"))),
        )
        assertEquals(
            listOf(some("Gmail notifications")),
            phrases(work, Target.ChatNamePattern(gmail, pattern("^Work"))),
        )
    }

    @Test
    fun `an app that only asks has no phrase`() {
        assertEquals(
            emptyList<HeldPhrase>(),
            HeldPhrases.of(rule(ruleApps(emptySet(), setOf(whatsapp))), labels::getValue),
        )
    }

    @Test
    fun `phrases list up to three then count the rest`() {
        assertEquals("Slack", HeldPhrases.listed("Slack", emptyList()))
        assertEquals("Slack and Gmail", HeldPhrases.listed("Slack", listOf("Gmail")))
        assertEquals("Slack, Gmail and Boss", HeldPhrases.listed("Slack", listOf("Gmail", "Boss")))
        assertEquals("a, b, c and 1 more", HeldPhrases.listed("a", listOf("b", "c", "d")))
        assertEquals("a, b, c and 2 more", HeldPhrases.listed("a", listOf("b", "c", "d", "e")))
    }

    @Test
    fun `a listed phrase is one only when it stands alone`() {
        assertEquals(one("Slack"), HeldPhrases.listed(one("Slack"), emptyList()))
        assertEquals(many("2 Slack categories"), HeldPhrases.listed(many("2 Slack categories"), emptyList()))
        assertEquals(many("Slack and Boss"), HeldPhrases.listed(one("Slack"), listOf(one("Boss"))))
    }

    @Test
    fun `a listed phrase opens with its first phrase's opening`() {
        assertEquals("Some Slack chats and Boss", HeldPhrases.listed(some("Slack chats"), listOf(one("Boss"))).opening)
    }

    @Test
    fun `the effect agrees with the phrase and follows the policy`() {
        val hours = "on weekdays 08:00–17:00"

        assertEquals(
            "Slack only reaches you on weekdays 08:00–17:00.",
            HeldPhrases.effect(one("Slack"), Policy.DeliverOnlyDuring, hours),
        )
        assertEquals(
            "2 Slack categories only reach you on weekdays 08:00–17:00.",
            HeldPhrases.effect(many("2 Slack categories"), Policy.DeliverOnlyDuring, hours),
        )
        assertEquals(
            "Slack goes quiet on weekdays 08:00–17:00.",
            HeldPhrases.effect(one("Slack"), Policy.HoldDuring, hours),
        )
        assertEquals(
            "2 Slack categories go quiet on weekdays 08:00–17:00.",
            HeldPhrases.effect(many("2 Slack categories"), Policy.HoldDuring, hours),
        )
    }

    private fun one(text: String): HeldPhrase = HeldPhrase.of(text, GrammaticalNumber.One)

    private fun many(text: String): HeldPhrase = HeldPhrase.of(text, GrammaticalNumber.Many)

    private fun some(text: String): HeldPhrase = HeldPhrase.some(text)

    private fun phrases(vararg targets: Target): List<HeldPhrase> =
        HeldPhrases.of(rule(ruleApps(targets.toSet(), emptySet())), labels::getValue)

    private fun rule(apps: RuleApps): Rule =
        Settings.DEFAULT.rules.rules
            .single()
            .copy(apps = apps)

    private fun account(
        app: PackageName,
        id: String,
        name: String,
    ): Target.Account = Target.Account(app, ChannelGroupId(id), groupName(name))

    private fun pattern(source: String): TextPattern =
        assertInstanceOf(TextPatternParse.Valid::class.java, TextPattern.parse(source)).pattern
}
