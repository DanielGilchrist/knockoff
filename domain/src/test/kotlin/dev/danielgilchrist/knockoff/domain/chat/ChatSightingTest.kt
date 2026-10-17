package dev.danielgilchrist.knockoff.domain.chat

import dev.danielgilchrist.knockoff.domain.COLLEAGUE
import dev.danielgilchrist.knockoff.domain.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.WHATSAPP
import dev.danielgilchrist.knockoff.domain.known
import dev.danielgilchrist.knockoff.domain.notification.Channel
import dev.danielgilchrist.knockoff.domain.notification.Conversation
import dev.danielgilchrist.knockoff.domain.notification.ConversationName
import dev.danielgilchrist.knockoff.domain.notification.ConversationTitle
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

    @Test
    fun `a conversation on a named channel is a chat`() {
        ChatSighting.of(posted(whatsappOrigin(known(COLLEAGUE))), emptyList()) shouldBe
            ChatSighting.Seen(COLLEAGUE_CHAT)
    }

    @Test
    fun `a notification without a conversation is not a chat`() {
        ChatSighting.of(posted(whatsappOrigin(Conversation.None)), emptyList()) shouldBe ChatSighting.NotAChat
    }

    @Test
    fun `a conversation without a channel is not a chat`() {
        val unnamed = Origin(WHATSAPP, Channel.Unnamed, known(COLLEAGUE))

        ChatSighting.of(posted(unnamed), emptyList()) shouldBe ChatSighting.NotAChat
    }

    @Test
    fun `an ongoing conversation such as a call is not a chat`() {
        val call =
            PostedNotification.Unsnoozable(
                key,
                whatsappOrigin(known(COLLEAGUE)),
                traits(NotificationTrait.Ongoing),
            )

        ChatSighting.of(call, emptyList()) shouldBe ChatSighting.NotAChat
    }

    @Test
    fun `a chat is named from its shortcut label before its title`() {
        val sighting =
            ChatSighting.of(
                posted(whatsappOrigin(known(COLLEAGUE, "Colleague (2 messages)"))),
                listOf("Colleague"),
            )

        sighting shouldBe ChatSighting.Seen(COLLEAGUE_CHAT)
    }

    @Test
    fun `an untitled chat without a label is named by its id`() {
        val untitled = whatsappOrigin(Conversation.Known(COLLEAGUE, ConversationTitle.Untitled))

        ChatSighting.of(posted(untitled), emptyList()) shouldBe
            ChatSighting.Seen(COLLEAGUE_CHAT.copy(name = ConversationName(COLLEAGUE.value)))
    }

    private fun posted(origin: Origin): PostedNotification = PostedNotification.Snoozable(key, origin)
}
