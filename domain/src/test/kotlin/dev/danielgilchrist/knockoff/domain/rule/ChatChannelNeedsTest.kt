package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.COLLEAGUE_CHANNEL
import dev.danielgilchrist.knockoff.domain.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.COLLEAGUE_KIND
import dev.danielgilchrist.knockoff.domain.FRIEND
import dev.danielgilchrist.knockoff.domain.SLACK
import dev.danielgilchrist.knockoff.domain.WHATSAPP
import dev.danielgilchrist.knockoff.domain.WHATSAPP_CHATS
import dev.danielgilchrist.knockoff.domain.WORK_ADDRESS
import dev.danielgilchrist.knockoff.domain.WORK_HOURS
import dev.danielgilchrist.knockoff.domain.channel.AlertingImportance
import dev.danielgilchrist.knockoff.domain.channel.ChannelGrouping
import dev.danielgilchrist.knockoff.domain.channel.ChannelKey
import dev.danielgilchrist.knockoff.domain.channel.ChannelKind
import dev.danielgilchrist.knockoff.domain.channel.Importance
import dev.danielgilchrist.knockoff.domain.channel.ReadChannel
import dev.danielgilchrist.knockoff.domain.channel.SilencedChannels
import dev.danielgilchrist.knockoff.domain.chat.ChatChannelNeed
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.ConversationName
import dev.danielgilchrist.knockoff.domain.rule
import dev.danielgilchrist.knockoff.domain.ruleSet
import dev.danielgilchrist.knockoff.domain.sharedChannels
import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.textPattern
import org.junit.jupiter.api.Test

class ChatChannelNeedsTest {
    private val friendChat = COLLEAGUE_CHAT.copy(id = FRIEND, name = ConversationName("Friend"))
    private val parent = ChannelKey(WHATSAPP, WHATSAPP_CHATS)
    private val shared = sharedChannels(parent to Importance.High)
    private val both = ruleSet(rule("Work", Policy.DeliverOnlyDuring, WORK_HOURS, setOf(COLLEAGUE_CHAT, friendChat)))

    @Test
    fun `chats without their own channel need one under their parent`() {
        both.chatChannelNeeds(shared, SilencedChannels.NONE) shouldBe
            setOf(ChatChannelNeed.Create(COLLEAGUE_CHAT), ChatChannelNeed.Create(friendChat))
    }

    @Test
    fun `a chat that already has its own channel needs none`() {
        val read =
            shared + (COLLEAGUE_CHANNEL to ReadChannel(Importance.High, COLLEAGUE_KIND, ChannelGrouping.Ungrouped))

        both.chatChannelNeeds(read, SilencedChannels.NONE) shouldBe setOf(ChatChannelNeed.Create(friendChat))
    }

    @Test
    fun `a channel for the chat under an old parent or in another app doesn't count`() {
        val oldParent = ChannelKind.ConversationOf(ChannelId("individual_chat_defaults_3"), COLLEAGUE_CHAT.id)
        val read =
            shared +
                mapOf(
                    COLLEAGUE_CHANNEL to ReadChannel(Importance.High, oldParent, ChannelGrouping.Ungrouped),
                    ChannelKey(SLACK, ChannelId("c1")) to
                        ReadChannel(Importance.High, COLLEAGUE_KIND, ChannelGrouping.Ungrouped),
                )
        val colleagueOnly = ruleSet(rule("Work", Policy.DeliverOnlyDuring, WORK_HOURS, setOf(COLLEAGUE_CHAT)))

        colleagueOnly.chatChannelNeeds(read, SilencedChannels.NONE) shouldBe
            setOf(ChatChannelNeed.Create(COLLEAGUE_CHAT))
    }

    @Test
    fun `a chat whose parent channel is gone can't have one`() {
        both.chatChannelNeeds(emptyMap(), SilencedChannels.NONE) shouldBe
            setOf(ChatChannelNeed.ParentMissing(COLLEAGUE_CHAT), ChatChannelNeed.ParentMissing(friendChat))
    }

    @Test
    fun `a chat waits while its parent is silenced so its channel doesn't copy the silence`() {
        val silenced = SilencedChannels.of(mapOf(parent to AlertingImportance.High))
        val quietParent = sharedChannels(parent to Importance.Low)

        both.chatChannelNeeds(quietParent, silenced) shouldBe
            setOf(ChatChannelNeed.ParentSilenced(COLLEAGUE_CHAT), ChatChannelNeed.ParentSilenced(friendChat))
    }

    @Test
    fun `the same chat in two rules needs one channel`() {
        val twice =
            ruleSet(
                rule("Work", Policy.DeliverOnlyDuring, WORK_HOURS, setOf(COLLEAGUE_CHAT)),
                rule("Evenings", Policy.HoldDuring, WORK_HOURS, setOf(COLLEAGUE_CHAT)),
            )

        twice.chatChannelNeeds(shared, SilencedChannels.NONE) shouldBe setOf(ChatChannelNeed.Create(COLLEAGUE_CHAT))
    }

    @Test
    fun `disabled rules and other targets need no chat channels`() {
        val rules =
            ruleSet(
                rule("Off", Policy.DeliverOnlyDuring, WORK_HOURS, setOf(COLLEAGUE_CHAT), RuleState.Disabled),
                rule(
                    "Apps",
                    Policy.DeliverOnlyDuring,
                    WORK_HOURS,
                    setOf(
                        Target.App(WHATSAPP),
                        Target.GmailInbox(WORK_ADDRESS),
                        Target.ChatNamePattern(WHATSAPP, textPattern(".")),
                        Target.ChannelPattern(WHATSAPP, textPattern(".")),
                    ),
                ),
            )

        rules.chatChannelNeeds(emptyMap(), SilencedChannels.NONE) shouldBe emptySet()
    }
}
