package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.COLLEAGUE
import dev.danielgilchrist.knockoff.domain.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.FRIEND
import dev.danielgilchrist.knockoff.domain.PERSONAL_GMAIL_ORIGIN
import dev.danielgilchrist.knockoff.domain.SLACK
import dev.danielgilchrist.knockoff.domain.SLACK_ORIGIN
import dev.danielgilchrist.knockoff.domain.WHATSAPP
import dev.danielgilchrist.knockoff.domain.WHATSAPP_CHATS
import dev.danielgilchrist.knockoff.domain.WORK_ADDRESS
import dev.danielgilchrist.knockoff.domain.WORK_GMAIL_ORIGIN
import dev.danielgilchrist.knockoff.domain.channel.ChannelKey
import dev.danielgilchrist.knockoff.domain.notification.Channel
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.Conversation
import dev.danielgilchrist.knockoff.domain.notification.Origin
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.whatsappOrigin
import org.junit.jupiter.api.Test

class TargetTest {
    @Test
    fun `app target matches only its own package`() {
        Target.App(SLACK).matches(SLACK_ORIGIN) shouldBe true
        Target.App(SLACK).matches(Origin(WHATSAPP, Channel.Unnamed, Conversation.None)) shouldBe false
        Target.App(SLACK).matches(WORK_GMAIL_ORIGIN) shouldBe false
    }

    @Test
    fun `targeting the gmail app matches every gmail account`() {
        val gmail = Target.App(PackageName.GMAIL)

        gmail.matches(WORK_GMAIL_ORIGIN) shouldBe true
        gmail.matches(PERSONAL_GMAIL_ORIGIN) shouldBe true
        gmail.matches(Origin(PackageName.GMAIL, Channel.Unnamed, Conversation.None)) shouldBe true
        gmail.matches(SLACK_ORIGIN) shouldBe false
    }

    @Test
    fun `gmail inbox target matches only that account's gmail channels`() {
        val inbox = Target.GmailInbox(WORK_ADDRESS)

        inbox.matches(WORK_GMAIL_ORIGIN) shouldBe true
        inbox.matches(PERSONAL_GMAIL_ORIGIN) shouldBe false
        inbox.matches(Origin(PackageName.GMAIL, Channel.Unnamed, Conversation.None)) shouldBe false
        inbox.matches(SLACK_ORIGIN) shouldBe false
    }

    @Test
    fun `chat target matches only that conversation in its app`() {
        COLLEAGUE_CHAT.matches(whatsappOrigin(Conversation.Known(COLLEAGUE))) shouldBe true
        COLLEAGUE_CHAT.matches(whatsappOrigin(Conversation.Known(FRIEND))) shouldBe false
        COLLEAGUE_CHAT.matches(whatsappOrigin(Conversation.None)) shouldBe false
        COLLEAGUE_CHAT.matches(Origin(SLACK, Channel.Named(WHATSAPP_CHATS), Conversation.Known(COLLEAGUE))) shouldBe
            false
    }

    @Test
    fun `chat target ignores which channel the conversation arrives on`() {
        val elsewhere = Origin(WHATSAPP, Channel.Unnamed, Conversation.Known(COLLEAGUE))

        COLLEAGUE_CHAT.matches(elsewhere) shouldBe true
    }

    @Test
    fun `app target matches every conversation in the app`() {
        Target.App(WHATSAPP).matches(whatsappOrigin(Conversation.Known(COLLEAGUE))) shouldBe true
    }

    @Test
    fun `an app target covers all of its channels`() {
        Target.App(WHATSAPP).covers(ChannelKey(WHATSAPP, WHATSAPP_CHATS)) shouldBe true
        Target.App(WHATSAPP).covers(ChannelKey(SLACK, WHATSAPP_CHATS)) shouldBe false
    }

    @Test
    fun `a gmail inbox target covers only that account's channels`() {
        val inbox = Target.GmailInbox(WORK_ADDRESS)

        inbox.covers(ChannelKey(PackageName.GMAIL, ChannelId("^nc_1_mail_dan@work.example"))) shouldBe true
        inbox.covers(ChannelKey(PackageName.GMAIL, ChannelId("^nc_1_mail_dan@home.example"))) shouldBe false
        inbox.covers(ChannelKey(SLACK, ChannelId("^nc_1_mail_dan@work.example"))) shouldBe false
    }

    @Test
    fun `a chat target never covers its app's shared channel`() {
        COLLEAGUE_CHAT.covers(ChannelKey(WHATSAPP, WHATSAPP_CHATS)) shouldBe false
    }

    @Test
    fun `gmail inbox target ignores other apps with a matching channel`() {
        val lookalike = Origin(SLACK, Channel.Named(ChannelId("^nc_1_mail_dan@work.example")), Conversation.None)

        Target.GmailInbox(WORK_ADDRESS).matches(lookalike) shouldBe false
    }
}
