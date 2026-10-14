package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.PERSONAL_GMAIL_ORIGIN
import dev.danielgilchrist.knockoff.domain.SLACK
import dev.danielgilchrist.knockoff.domain.SLACK_ORIGIN
import dev.danielgilchrist.knockoff.domain.WORK_ADDRESS
import dev.danielgilchrist.knockoff.domain.WORK_GMAIL_ORIGIN
import dev.danielgilchrist.knockoff.domain.notification.Channel
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.Origin
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.shouldBe
import org.junit.jupiter.api.Test

class TargetTest {
    @Test
    fun `app target matches only its own package`() {
        Target.App(SLACK).matches(SLACK_ORIGIN) shouldBe true
        Target.App(SLACK).matches(Origin(PackageName("com.whatsapp"), Channel.Unnamed)) shouldBe false
        Target.App(SLACK).matches(WORK_GMAIL_ORIGIN) shouldBe false
    }

    @Test
    fun `targeting the gmail app matches every gmail account`() {
        val gmail = Target.App(PackageName.GMAIL)

        gmail.matches(WORK_GMAIL_ORIGIN) shouldBe true
        gmail.matches(PERSONAL_GMAIL_ORIGIN) shouldBe true
        gmail.matches(Origin(PackageName.GMAIL, Channel.Unnamed)) shouldBe true
        gmail.matches(SLACK_ORIGIN) shouldBe false
    }

    @Test
    fun `gmail inbox target matches only that account's gmail channels`() {
        val inbox = Target.GmailInbox(WORK_ADDRESS)

        inbox.matches(WORK_GMAIL_ORIGIN) shouldBe true
        inbox.matches(PERSONAL_GMAIL_ORIGIN) shouldBe false
        inbox.matches(Origin(PackageName.GMAIL, Channel.Unnamed)) shouldBe false
        inbox.matches(SLACK_ORIGIN) shouldBe false
    }

    @Test
    fun `gmail inbox target ignores other apps with a matching channel`() {
        val lookalike = Origin(SLACK, Channel.Named(ChannelId("^nc_1_mail_dan@work.example")))

        Target.GmailInbox(WORK_ADDRESS).matches(lookalike) shouldBe false
    }
}
