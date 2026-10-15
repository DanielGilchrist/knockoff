package dev.danielgilchrist.knockoff.domain.channel

import dev.danielgilchrist.knockoff.domain.SLACK
import dev.danielgilchrist.knockoff.domain.SLACK_ORIGIN
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.shouldBe
import org.junit.jupiter.api.Test

class ChannelKeyTest {
    private val key = ChannelKey(SLACK, ChannelId("messages"))

    @Test
    fun `a channel key is the origin of notifications on that channel`() {
        key.origin shouldBe SLACK_ORIGIN
    }

    @Test
    fun `keys and changes describe themselves`() {
        key.describe() shouldBe "com.Slack/messages"
        ChannelChange(key, Importance.Default, Importance.Low).describe() shouldBe "com.Slack/messages Default -> Low"
    }
}
