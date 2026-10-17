package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.COLLEAGUE
import dev.danielgilchrist.knockoff.domain.COLLEAGUE_CHANNEL
import dev.danielgilchrist.knockoff.domain.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.COLLEAGUE_KIND
import dev.danielgilchrist.knockoff.domain.FRIEND
import dev.danielgilchrist.knockoff.domain.PERSONAL_GMAIL_ORIGIN
import dev.danielgilchrist.knockoff.domain.SLACK
import dev.danielgilchrist.knockoff.domain.SLACK_ORIGIN
import dev.danielgilchrist.knockoff.domain.WHATSAPP
import dev.danielgilchrist.knockoff.domain.WHATSAPP_CHATS
import dev.danielgilchrist.knockoff.domain.WORK_ADDRESS
import dev.danielgilchrist.knockoff.domain.WORK_GMAIL_ORIGIN
import dev.danielgilchrist.knockoff.domain.channel.ChannelKey
import dev.danielgilchrist.knockoff.domain.channel.ChannelKind
import dev.danielgilchrist.knockoff.domain.known
import dev.danielgilchrist.knockoff.domain.notification.Channel
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.Conversation
import dev.danielgilchrist.knockoff.domain.notification.ConversationName
import dev.danielgilchrist.knockoff.domain.notification.ConversationTitle
import dev.danielgilchrist.knockoff.domain.notification.Origin
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.textPattern
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
        COLLEAGUE_CHAT.matches(whatsappOrigin(known(COLLEAGUE))) shouldBe true
        COLLEAGUE_CHAT.matches(whatsappOrigin(known(FRIEND))) shouldBe false
        COLLEAGUE_CHAT.matches(whatsappOrigin(Conversation.None)) shouldBe false
        COLLEAGUE_CHAT.matches(Origin(SLACK, Channel.Named(WHATSAPP_CHATS), known(COLLEAGUE))) shouldBe
            false
    }

    @Test
    fun `chat target ignores which channel the conversation arrives on`() {
        val elsewhere = Origin(WHATSAPP, Channel.Unnamed, known(COLLEAGUE))

        COLLEAGUE_CHAT.matches(elsewhere) shouldBe true
    }

    @Test
    fun `app target matches every conversation in the app`() {
        Target.App(WHATSAPP).matches(whatsappOrigin(known(COLLEAGUE))) shouldBe true
    }

    @Test
    fun `an app target covers all of its channels, conversations included`() {
        val app = Target.App(WHATSAPP)
        val shared = ChannelKey(WHATSAPP, WHATSAPP_CHATS)

        app.covers(shared, ChannelKind.Shared) shouldBe true
        app.covers(COLLEAGUE_CHANNEL, COLLEAGUE_KIND) shouldBe true
        app.covers(ChannelKey(SLACK, WHATSAPP_CHATS), ChannelKind.Shared) shouldBe false
    }

    @Test
    fun `a gmail inbox target covers only that account's channels`() {
        val inbox = Target.GmailInbox(WORK_ADDRESS)

        inbox.covers(
            ChannelKey(PackageName.GMAIL, ChannelId("^nc_1_mail_dan@work.example")),
            ChannelKind.Shared,
        ) shouldBe
            true
        inbox.covers(
            ChannelKey(PackageName.GMAIL, ChannelId("^nc_1_mail_dan@home.example")),
            ChannelKind.Shared,
        ) shouldBe
            false
        inbox.covers(ChannelKey(SLACK, ChannelId("^nc_1_mail_dan@work.example")), ChannelKind.Shared) shouldBe false
    }

    @Test
    fun `a chat target covers only its own conversation's channel under its parent`() {
        COLLEAGUE_CHAT.covers(COLLEAGUE_CHANNEL, COLLEAGUE_KIND) shouldBe true
        COLLEAGUE_CHAT.covers(ChannelKey(WHATSAPP, WHATSAPP_CHATS), ChannelKind.Shared) shouldBe false
        COLLEAGUE_CHAT.covers(COLLEAGUE_CHANNEL, ChannelKind.ConversationOf(WHATSAPP_CHATS, FRIEND)) shouldBe false
        COLLEAGUE_CHAT.covers(
            COLLEAGUE_CHANNEL,
            ChannelKind.ConversationOf(ChannelId("individual_chat_defaults_3"), COLLEAGUE),
        ) shouldBe
            false
        COLLEAGUE_CHAT.covers(ChannelKey(SLACK, ChannelId("c1")), COLLEAGUE_KIND) shouldBe false
    }

    @Test
    fun `gmail inbox target ignores other apps with a matching channel`() {
        val lookalike = Origin(SLACK, Channel.Named(ChannelId("^nc_1_mail_dan@work.example")), Conversation.None)

        Target.GmailInbox(WORK_ADDRESS).matches(lookalike) shouldBe false
    }

    @Test
    fun `a channel pattern matches and covers its app's channels whose id matches`() {
        val accounts = Target.ChannelPattern(PackageName.GMAIL, textPattern("_dan@work\\.example$"))

        accounts.matches(WORK_GMAIL_ORIGIN) shouldBe true
        accounts.matches(PERSONAL_GMAIL_ORIGIN) shouldBe false
        accounts.matches(Origin(PackageName.GMAIL, Channel.Unnamed, Conversation.None)) shouldBe false
        accounts.matches(
            Origin(SLACK, Channel.Named(ChannelId("^nc_1_mail_dan@work.example")), Conversation.None),
        ) shouldBe
            false
        accounts.covers(
            ChannelKey(PackageName.GMAIL, ChannelId("^nc_1_mail_dan@work.example")),
            ChannelKind.Shared,
        ) shouldBe
            true
        accounts.covers(
            ChannelKey(PackageName.GMAIL, ChannelId("^nc_1_mail_dan@home.example")),
            ChannelKind.Shared,
        ) shouldBe
            false
        accounts.covers(ChannelKey(SLACK, ChannelId("^nc_1_mail_dan@work.example")), ChannelKind.Shared) shouldBe false
    }

    @Test
    fun `a channel pattern covers conversation channels by their parent's id`() {
        val chats = Target.ChannelPattern(WHATSAPP, textPattern("^individual_chat"))

        chats.covers(COLLEAGUE_CHANNEL, COLLEAGUE_KIND) shouldBe true
        chats.covers(
            ChannelKey(WHATSAPP, ChannelId("individual_chat_x")),
            ChannelKind.ConversationOf(ChannelId("group_chat"), COLLEAGUE),
        ) shouldBe
            false
    }

    @Test
    fun `a chat name pattern matches conversations by name in its app`() {
        val work = Target.ChatNamePattern(WHATSAPP, textPattern("^Work"))

        work.matches(whatsappOrigin(known(COLLEAGUE, "Work – Sydney"))) shouldBe true
        work.matches(whatsappOrigin(known(COLLEAGUE, "Mum"))) shouldBe false
        work.matches(whatsappOrigin(Conversation.None)) shouldBe false
        work.matches(whatsappOrigin(Conversation.Known(COLLEAGUE, ConversationTitle.Untitled))) shouldBe false
        work.matches(Origin(SLACK, Channel.Named(WHATSAPP_CHATS), known(COLLEAGUE, "Work"))) shouldBe false
    }

    @Test
    fun `a chat name pattern covers no channel by itself`() {
        val work = Target.ChatNamePattern(WHATSAPP, textPattern("^Work"))

        work.covers(ChannelKey(WHATSAPP, WHATSAPP_CHATS), ChannelKind.Shared) shouldBe false
        work.covers(COLLEAGUE_CHANNEL, COLLEAGUE_KIND) shouldBe false
    }

    @Test
    fun `only chat name patterns expand to seen chats`() {
        val named = COLLEAGUE_CHAT.copy(name = ConversationName("Work chat"))

        Target.ChatNamePattern(WHATSAPP, textPattern("^Work")).expandsTo(named) shouldBe true
        Target.ChatNamePattern(WHATSAPP, textPattern("^Work")).expandsTo(COLLEAGUE_CHAT) shouldBe false
        Target.ChatNamePattern(SLACK, textPattern("^Work")).expandsTo(named) shouldBe false
        Target.App(WHATSAPP).expandsTo(named) shouldBe false
    }
}
