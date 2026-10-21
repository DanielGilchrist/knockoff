package dev.danielgilchrist.knockoff.domain.chat

import dev.danielgilchrist.knockoff.domain.COLLEAGUE
import dev.danielgilchrist.knockoff.domain.COLLEAGUE_CHANNEL
import dev.danielgilchrist.knockoff.domain.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.COLLEAGUE_KIND
import dev.danielgilchrist.knockoff.domain.FRIEND
import dev.danielgilchrist.knockoff.domain.LONDON
import dev.danielgilchrist.knockoff.domain.SLACK
import dev.danielgilchrist.knockoff.domain.WHATSAPP
import dev.danielgilchrist.knockoff.domain.WHATSAPP_CHATS
import dev.danielgilchrist.knockoff.domain.WORK_ADDRESS
import dev.danielgilchrist.knockoff.domain.WORK_HOURS
import dev.danielgilchrist.knockoff.domain.channel.AlertingImportance
import dev.danielgilchrist.knockoff.domain.channel.ChannelChange
import dev.danielgilchrist.knockoff.domain.channel.ChannelGrouping
import dev.danielgilchrist.knockoff.domain.channel.ChannelKey
import dev.danielgilchrist.knockoff.domain.channel.Importance
import dev.danielgilchrist.knockoff.domain.channel.ReadChannel
import dev.danielgilchrist.knockoff.domain.channel.SilencedChannels
import dev.danielgilchrist.knockoff.domain.group.GroupName
import dev.danielgilchrist.knockoff.domain.group.KnownGroups
import dev.danielgilchrist.knockoff.domain.known
import dev.danielgilchrist.knockoff.domain.london
import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.ConversationId
import dev.danielgilchrist.knockoff.domain.notification.ConversationName
import dev.danielgilchrist.knockoff.domain.notification.GroupRole
import dev.danielgilchrist.knockoff.domain.notification.NotificationKey
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.notification.PostedNotification
import dev.danielgilchrist.knockoff.domain.rule
import dev.danielgilchrist.knockoff.domain.rule.Decision
import dev.danielgilchrist.knockoff.domain.rule.Evaluation
import dev.danielgilchrist.knockoff.domain.rule.LeaveReason
import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.domain.rule.Recheck
import dev.danielgilchrist.knockoff.domain.rule.RuleApps
import dev.danielgilchrist.knockoff.domain.rule.RuleState
import dev.danielgilchrist.knockoff.domain.rule.Target
import dev.danielgilchrist.knockoff.domain.ruleApps
import dev.danielgilchrist.knockoff.domain.ruleName
import dev.danielgilchrist.knockoff.domain.ruleSet
import dev.danielgilchrist.knockoff.domain.sharedChannels
import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.shouldBeInstanceOf
import dev.danielgilchrist.knockoff.domain.textPattern
import dev.danielgilchrist.knockoff.domain.whatsappOrigin
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class SeenChatsTest {
    private val friend = COLLEAGUE_CHAT.copy(id = FRIEND, name = ConversationName("Friend"))

    @Test
    fun `the latest chat comes first`() {
        SeenChats.NONE
            .seen(COLLEAGUE_CHAT)
            .seen(friend)
            .chats shouldBe listOf(friend, COLLEAGUE_CHAT)
    }

    @Test
    fun `seeing a chat again moves it first with its latest name and channel`() {
        val renamed =
            COLLEAGUE_CHAT.copy(
                channel = ChannelId("individual_chat_defaults_5"),
                name = ConversationName("Boss"),
            )

        SeenChats.NONE
            .seen(COLLEAGUE_CHAT)
            .seen(friend)
            .seen(renamed)
            .chats shouldBe listOf(renamed, friend)
    }

    @Test
    fun `only the latest chats are kept`() {
        val many =
            (1..SeenChats.LIMIT + 1).fold(SeenChats.NONE) { seen, number ->
                seen.seen(COLLEAGUE_CHAT.copy(id = ConversationId("$number@lid")))
            }

        many.chats.size shouldBe SeenChats.LIMIT
        many.chats.first().id shouldBe ConversationId("${SeenChats.LIMIT + 1}@lid")
        many.chats.last().id shouldBe ConversationId("2@lid")
    }

    @Test
    fun `seen chats round-trip`() {
        val seen = SeenChats.NONE.seen(COLLEAGUE_CHAT).seen(friend)

        SeenChats.fromJson(seen.toJson()) shouldBe SeenChatsDecode.Decoded(seen)
    }

    @Test
    fun `the stored document is stable`() {
        SeenChats.NONE.seen(COLLEAGUE_CHAT).toJson() shouldBe
            """
            {
                "version": 1,
                "chats": [
                    {
                        "app": "com.whatsapp",
                        "channel": "individual_chat_defaults_4",
                        "id": "1000000000001@lid",
                        "name": "Colleague"
                    }
                ]
            }
            """.trimIndent()
    }

    @Test
    fun `chats named by their id are dropped when read`() {
        val named = """{"app": "com.whatsapp", "channel": "chats", "id": "1@lid", "name": "Colleague"}"""
        val byId = """{"app": "com.whatsapp", "channel": "chats", "id": "2@lid", "name": "2@lid"}"""

        SeenChats
            .fromJson("""{"version": 1, "chats": [$named, $byId]}""")
            .shouldBeInstanceOf<SeenChatsDecode.Decoded>()
            .seen.chats
            .map { it.id.value } shouldBe listOf("1@lid")
    }

    @Test
    fun `a seen chat expanded into a rule is held by id whatever its title says`() {
        val rules =
            ruleSet(
                rule(
                    "Work",
                    Policy.DeliverOnlyDuring,
                    WORK_HOURS,
                    setOf(Target.ChatNamePattern(WHATSAPP, textPattern("^Work"))),
                ),
            )
        val seen = SeenChats.NONE.seen(COLLEAGUE_CHAT.copy(name = ConversationName("Work chat")))
        val post =
            PostedNotification.Snoozable(
                NotificationKey("k"),
                whatsappOrigin(known(COLLEAGUE, "Somebody (3 messages)")),
            )
        val evening = london("2026-10-14T20:00")

        Evaluation(
            seen.expand(rules),
            LONDON,
        ).decide(post, GroupRole.Member, evening.toInstant()).shouldBeInstanceOf<Decision.Snooze>()
        Evaluation(rules, LONDON).decide(post, GroupRole.Member, evening.toInstant()) shouldBe
            Decision.Leave(LeaveReason.NoRuleTargets)
    }

    @Test
    fun `renaming a seen chat out of a pattern releases its snoozed notifications`() {
        val rules =
            ruleSet(
                rule(
                    "Work",
                    Policy.DeliverOnlyDuring,
                    WORK_HOURS,
                    setOf(Target.ChatNamePattern(WHATSAPP, textPattern("^Work"))),
                ),
            )
        val before =
            Evaluation(
                SeenChats.NONE.seen(COLLEAGUE_CHAT.copy(name = ConversationName("Work chat"))).expand(rules),
                LONDON,
            )
        val after =
            Evaluation(
                SeenChats.NONE.seen(COLLEAGUE_CHAT.copy(name = ConversationName("Family"))).expand(rules),
                LONDON,
            )
        val snoozed = PostedNotification.Snoozable(NotificationKey("k"), whatsappOrigin(known(COLLEAGUE, "Family")))

        after
            .recheck(
                snoozed,
                GroupRole.Member,
                before,
                london("2026-10-14T20:00").toInstant(),
            ).shouldBeInstanceOf<Recheck.Release>()
    }

    @Test
    fun `reordering seen chats leaves the expanded rules equal`() {
        val rules =
            ruleSet(
                rule(
                    "Work",
                    Policy.DeliverOnlyDuring,
                    WORK_HOURS,
                    setOf(Target.ChatNamePattern(WHATSAPP, textPattern(".")), COLLEAGUE_CHAT),
                ),
            )

        SeenChats.NONE
            .seen(COLLEAGUE_CHAT)
            .seen(friend)
            .expand(rules) shouldBe
            SeenChats.NONE
                .seen(friend)
                .seen(COLLEAGUE_CHAT)
                .expand(rules)
    }

    @ParameterizedTest(name = "{4}")
    @CsvSource(
        "' ', chats, 1@lid, Someone, blank chat app",
        "com.whatsapp, ' ', 1@lid, Someone, blank chat channel",
        "com.whatsapp, chats, ' ', Someone, blank chat id",
        "com.whatsapp, chats, 1@lid, ' ', blank chat name",
    )
    fun `an invalid chat is reported`(
        app: String,
        channel: String,
        id: String,
        name: String,
        problem: String,
    ) {
        val chat = """{"app": "$app", "channel": "$channel", "id": "$id", "name": "$name"}"""

        SeenChats
            .fromJson("""{"version": 1, "chats": [$chat]}""")
            .shouldBeInstanceOf<SeenChatsDecode.Invalid>()
            .detail shouldBe "chat 1: $problem"
    }

    @Test
    fun `other versions are unsupported`() {
        val decode = SeenChats.fromJson("""{"version": 2, "chats": []}""")

        decode.shouldBeInstanceOf<SeenChatsDecode.UnsupportedVersion>().version shouldBe 2
        decode.shouldBeInstanceOf<SeenChatsDecode.Failure>().detail shouldBe "Unsupported seen chats version 2"
    }

    @Test
    fun `an unparseable document is unreadable`() {
        val detail = SeenChats.fromJson("{nope").shouldBeInstanceOf<SeenChatsDecode.Unreadable>().detail

        detail.take("kotlinx.serialization.".length) shouldBe "kotlinx.serialization."
    }

    @Test
    fun `chat name patterns take in the seen chats whose names match`() {
        val work = friend.copy(name = ConversationName("Work – Sydney"))
        val elsewhere = work.copy(packageName = SLACK)
        val pattern = Target.ChatNamePattern(WHATSAPP, textPattern("^Work"))
        val rules = ruleSet(rule("Work", Policy.DeliverOnlyDuring, WORK_HOURS, setOf(pattern, COLLEAGUE_CHAT)))
        val seen =
            SeenChats.NONE
                .seen(
                    work,
                ).seen(elsewhere)
                .seen(COLLEAGUE_CHAT.copy(name = ConversationName("Work chat")))

        seen
            .expand(rules)
            .rules
            .single()
            .targets shouldBe setOf(pattern, COLLEAGUE_CHAT, work)
    }

    @Test
    fun `rules without chat name patterns are unchanged`() {
        val rules =
            ruleSet(
                rule(
                    "Work",
                    Policy.DeliverOnlyDuring,
                    WORK_HOURS,
                    setOf(Target.App(WHATSAPP), Target.ChannelPattern(WHATSAPP, textPattern("."))),
                ),
            )

        SeenChats.NONE.seen(friend).expand(rules) shouldBe rules
    }

    @Test
    fun `a picked chat takes its channel from its latest sighting`() {
        val moved = COLLEAGUE_CHAT.copy(channel = ChannelId("individual_chat_defaults_5"))
        val rules = ruleSet(rule("Work", Policy.DeliverOnlyDuring, WORK_HOURS, setOf(COLLEAGUE_CHAT)))

        SeenChats.NONE
            .seen(moved)
            .expand(rules)
            .rules
            .single()
            .targets shouldBe setOf(moved)
        SeenChats.NONE.seen(friend).expand(rules) shouldBe rules
    }

    @Test
    fun `a chat renamed out of a pattern has its channel restored`() {
        val pattern = Target.ChatNamePattern(WHATSAPP, textPattern("^Work"))
        val rules = ruleSet(rule("Work", Policy.DeliverOnlyDuring, WORK_HOURS, setOf(pattern)))
        val read = mapOf(COLLEAGUE_CHANNEL to ReadChannel(Importance.Low, COLLEAGUE_KIND, ChannelGrouping.Ungrouped))
        val silenced = SilencedChannels.of(mapOf(COLLEAGUE_CHANNEL to AlertingImportance.High))
        val evening = london("2026-10-14T20:00")
        val working = SeenChats.NONE.seen(COLLEAGUE_CHAT.copy(name = ConversationName("Work chat")))
        val renamed = SeenChats.NONE.seen(COLLEAGUE_CHAT.copy(name = ConversationName("Family")))

        working.expand(rules).planChannels(read, silenced, evening).restore shouldBe emptyList()
        renamed.expand(rules).planChannels(read, silenced, evening).restore shouldBe
            listOf(ChannelChange(COLLEAGUE_CHANNEL, Importance.Low, Importance.High))
    }

    @Test
    fun `undecided chats are those asked about, in no rule and not let through, newest first`() {
        val work = friend.copy(name = ConversationName("Work – Sydney"))
        val other = friend.copy(id = ConversationId("3@lid"), name = ConversationName("Mum"))
        val dad = friend.copy(id = ConversationId("4@lid"), name = ConversationName("Dad"))
        val rules =
            ruleSet(
                rule(
                    "Work",
                    Policy.DeliverOnlyDuring,
                    WORK_HOURS,
                    setOf(COLLEAGUE_CHAT, Target.ChatNamePattern(WHATSAPP, textPattern("^Work"))),
                    asking = setOf(WHATSAPP),
                ),
            )
        val seen =
            SeenChats.NONE
                .seen(COLLEAGUE_CHAT)
                .seen(work)
                .seen(other)
                .seen(dad)
        val decisions = ChatDecisions.NONE.lettingThrough(other.key)

        seen.undecided(rules, decisions, KnownGroups.NONE) shouldBe listOf(dad)
        seen.undecided(rules, ChatDecisions.NONE, KnownGroups.NONE) shouldBe listOf(dad, other)
    }

    @Test
    fun `chats held by any target are not undecided`() {
        val seen = SeenChats.NONE.seen(COLLEAGUE_CHAT)
        val heldBy = { target: Target ->
            seen.undecided(
                ruleSet(
                    rule("Work", Policy.DeliverOnlyDuring, WORK_HOURS, setOf(target), RuleState.Disabled),
                    rule("Ask", Policy.DeliverOnlyDuring, WORK_HOURS, emptySet(), asking = setOf(WHATSAPP)),
                ),
                ChatDecisions.NONE,
                KnownGroups.NONE,
            )
        }

        heldBy(Target.App(WHATSAPP)) shouldBe emptyList()
        heldBy(Target.ChannelPattern(WHATSAPP, textPattern("^individual"))) shouldBe emptyList()
        heldBy(Target.ChannelPattern(WHATSAPP, textPattern("^group"))) shouldBe listOf(COLLEAGUE_CHAT)
        heldBy(Target.App(SLACK)) shouldBe listOf(COLLEAGUE_CHAT)
        heldBy(COLLEAGUE_CHAT.copy(packageName = SLACK)) shouldBe listOf(COLLEAGUE_CHAT)
    }

    @Test
    fun `chats from apps no enabled rule asks about are not undecided`() {
        val seen = SeenChats.NONE.seen(COLLEAGUE_CHAT)
        val undecided = { state: RuleState, apps: Set<PackageName> ->
            seen.undecided(
                ruleSet(rule("Work", Policy.DeliverOnlyDuring, WORK_HOURS, emptySet(), state, asking = apps)),
                ChatDecisions.NONE,
                KnownGroups.NONE,
            )
        }

        undecided(RuleState.Enabled, emptySet()) shouldBe emptyList()
        undecided(RuleState.Enabled, setOf(SLACK)) shouldBe emptyList()
        undecided(RuleState.Disabled, setOf(WHATSAPP)) shouldBe emptyList()
        undecided(RuleState.Enabled, setOf(WHATSAPP)) shouldBe listOf(COLLEAGUE_CHAT)
    }

    @Test
    fun `a chat in a group an account rule holds is not undecided`() {
        val group = ChannelGroupId("channel_group_chats")
        val seen = SeenChats.NONE.seen(COLLEAGUE_CHAT)
        val rules =
            ruleSet(
                rule(
                    "Work",
                    Policy.DeliverOnlyDuring,
                    WORK_HOURS,
                    setOf(Target.Account(WHATSAPP, group, WORK_ADDRESS)),
                    asking = setOf(WHATSAPP),
                ),
            )
        val known =
            KnownGroups.NONE.sighted(
                ChannelKey(COLLEAGUE_CHAT.packageName, COLLEAGUE_CHAT.channel),
                ChannelGrouping.Grouped(group),
            )

        seen.undecided(rules, ChatDecisions.NONE, KnownGroups.NONE) shouldBe listOf(COLLEAGUE_CHAT)
        seen.undecided(rules, ChatDecisions.NONE, known) shouldBe emptyList()
    }

    @Test
    fun `nothing seen means nothing undecided`() {
        SeenChats.NONE.undecided(ruleSet(), ChatDecisions.NONE, KnownGroups.NONE) shouldBe emptyList()
    }

    @Test
    fun `undecided chats not yet asked about are questions for the enabled rules asking about their app`() {
        val asking = { name: String, state: RuleState, apps: Set<PackageName> ->
            rule(name, Policy.DeliverOnlyDuring, WORK_HOURS, emptySet(), state, asking = apps)
        }
        val slack = friend.copy(packageName = SLACK)
        val seen =
            SeenChats.NONE
                .seen(COLLEAGUE_CHAT)
                .seen(friend)
                .seen(slack)
        val rules =
            ruleSet(
                asking("Work", RuleState.Enabled, setOf(WHATSAPP)),
                asking("Clients", RuleState.Enabled, setOf(WHATSAPP, SLACK)),
                asking("Old", RuleState.Disabled, setOf(WHATSAPP)),
            )
        val decisions = ChatDecisions.NONE.askedAbout(setOf(COLLEAGUE_CHAT.key))

        seen.questions(rules, decisions, KnownGroups.NONE) shouldBe
            listOf(
                ChatQuestion(slack, ruleName("Clients")),
                ChatQuestion(friend, ruleName("Work"), listOf(ruleName("Clients"))),
            )
        seen.questions(
            ruleSet(asking("Work", RuleState.Enabled, emptySet())),
            ChatDecisions.NONE,
            KnownGroups.NONE,
        ) shouldBe
            emptyList()
        seen.questions(rules, decisions.lettingThrough(friend.key).lettingThrough(slack.key), KnownGroups.NONE) shouldBe
            emptyList()
    }

    @Test
    fun `a chat held by any rule is never a question even when another rule asks about its app`() {
        val seen = SeenChats.NONE.seen(COLLEAGUE_CHAT)
        val asker = rule("Ask", Policy.DeliverOnlyDuring, WORK_HOURS, emptySet(), asking = setOf(WHATSAPP))
        val holding = { target: Target ->
            seen.questions(
                ruleSet(asker, rule("Hold", Policy.DeliverOnlyDuring, WORK_HOURS, setOf(target))),
                ChatDecisions.NONE,
                KnownGroups.NONE,
            )
        }

        seen.questions(ruleSet(asker), ChatDecisions.NONE, KnownGroups.NONE) shouldBe
            listOf(ChatQuestion(COLLEAGUE_CHAT, asker.name))
        holding(COLLEAGUE_CHAT) shouldBe emptyList()
        holding(Target.ChatNamePattern(WHATSAPP, textPattern("^Coll"))) shouldBe emptyList()
        holding(Target.App(WHATSAPP)) shouldBe emptyList()
        seen.questions(
            ruleSet(asker.copy(apps = ruleApps(setOf(COLLEAGUE_CHAT), asker.asking))),
            ChatDecisions.NONE,
            KnownGroups.NONE,
        ) shouldBe
            emptyList()
    }
}
