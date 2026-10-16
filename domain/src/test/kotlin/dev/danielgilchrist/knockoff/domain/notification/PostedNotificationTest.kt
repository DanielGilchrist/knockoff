package dev.danielgilchrist.knockoff.domain.notification

import dev.danielgilchrist.knockoff.domain.SLACK
import dev.danielgilchrist.knockoff.domain.WORK_GMAIL_ORIGIN
import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.traits
import org.junit.jupiter.api.Test

class PostedNotificationTest {
    private val key = NotificationKey("0|com.google.android.gm|7|gig|10206")

    @Test
    fun `snoozable notifications describe their key, package and channel`() {
        PostedNotification.Snoozable(key, WORK_GMAIL_ORIGIN).describe() shouldBe
            "key=0|com.google.android.gm|7|gig|10206 package=com.google.android.gm channel=^nc_1_mail_dan@work.example"
    }

    @Test
    fun `unsnoozable notifications without a channel say so`() {
        PostedNotification
            .Unsnoozable(key, Origin(SLACK, Channel.Unnamed, Conversation.None), traits(NotificationTrait.Ongoing))
            .describe() shouldBe "key=0|com.google.android.gm|7|gig|10206 package=com.Slack channel=(none)"
    }
}
