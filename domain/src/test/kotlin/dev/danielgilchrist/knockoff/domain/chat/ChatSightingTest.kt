package dev.danielgilchrist.knockoff.domain.chat

import dev.danielgilchrist.knockoff.domain.COLLEAGUE
import dev.danielgilchrist.knockoff.domain.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.WHATSAPP
import dev.danielgilchrist.knockoff.domain.notification.Channel
import dev.danielgilchrist.knockoff.domain.notification.Conversation
import dev.danielgilchrist.knockoff.domain.notification.NotificationKey
import dev.danielgilchrist.knockoff.domain.notification.NotificationTrait
import dev.danielgilchrist.knockoff.domain.notification.Origin
import dev.danielgilchrist.knockoff.domain.notification.PostedNotification
import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.traits
import dev.danielgilchrist.knockoff.domain.whatsappOrigin
import org.junit.jupiter.api.Test

class ChatSightingTest {
    private val key = NotificationKey("0|com.whatsapp|1|chat|10395")
    private val names = listOf(COLLEAGUE_CHAT.name.value)

    @Test
    fun `a conversation on a named channel is a chat`() {
        ChatSighting.of(posted(whatsappOrigin(Conversation.Known(COLLEAGUE))), names) shouldBe
            ChatSighting.Seen(COLLEAGUE_CHAT)
    }

    @Test
    fun `a notification without a conversation is not a chat`() {
        ChatSighting.of(posted(whatsappOrigin(Conversation.None)), names) shouldBe ChatSighting.NotAChat
    }

    @Test
    fun `a conversation without a channel is not a chat`() {
        val unnamed = Origin(WHATSAPP, Channel.Unnamed, Conversation.Known(COLLEAGUE))

        ChatSighting.of(posted(unnamed), names) shouldBe ChatSighting.NotAChat
    }

    @Test
    fun `an ongoing conversation such as a call is not a chat`() {
        val call =
            PostedNotification.Unsnoozable(
                key,
                whatsappOrigin(Conversation.Known(COLLEAGUE)),
                traits(NotificationTrait.Ongoing),
            )

        ChatSighting.of(call, names) shouldBe ChatSighting.NotAChat
    }

    private fun posted(origin: Origin): PostedNotification = PostedNotification.Snoozable(key, origin)
}
