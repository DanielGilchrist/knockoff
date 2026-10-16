package dev.danielgilchrist.knockoff.domain.notification

import dev.danielgilchrist.knockoff.domain.COLLEAGUE
import dev.danielgilchrist.knockoff.domain.SLACK
import dev.danielgilchrist.knockoff.domain.WHATSAPP
import dev.danielgilchrist.knockoff.domain.WHATSAPP_CHATS
import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.traits
import dev.danielgilchrist.knockoff.domain.whatsappOrigin
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

class NotificationSnapshotTest {
    private val key = NotificationKey("0|com.Slack|1|null|10001")

    @Test
    fun `an app notification without blocking traits is snoozable`() {
        snapshot(SLACK, "messages").parse() shouldBe
            PostedNotification.Snoozable(key, Origin(SLACK, Channel.Named(ChannelId("messages")), Conversation.None))
    }

    @Test
    fun `a gmail notification keeps its account channel`() {
        snapshot(PackageName.GMAIL, "^nc_1_mail_dan@work.example").parse() shouldBe
            PostedNotification.Snoozable(
                key,
                Origin(PackageName.GMAIL, Channel.Named(ChannelId("^nc_1_mail_dan@work.example")), Conversation.None),
            )
    }

    @Test
    fun `a notification without a channel is unnamed`() {
        snapshot(SLACK, channelId = null).parse() shouldBe
            PostedNotification.Snoozable(key, Origin(SLACK, Channel.Unnamed, Conversation.None))
    }

    @Test
    fun `a notification with a shortcut belongs to that conversation`() {
        snapshot(WHATSAPP, WHATSAPP_CHATS.value, shortcutId = COLLEAGUE.value).parse() shouldBe
            PostedNotification.Snoozable(key, whatsappOrigin(Conversation.Known(COLLEAGUE)))
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " "])
    fun `a blank shortcut is no conversation`(shortcutId: String) {
        snapshot(WHATSAPP, WHATSAPP_CHATS.value, shortcutId = shortcutId).parse().origin.conversation shouldBe
            Conversation.None
    }

    @ParameterizedTest
    @EnumSource(NotificationTrait::class)
    fun `any blocking trait makes it unsnoozable`(trait: NotificationTrait) {
        snapshot(SLACK, "messages", setOf(trait)).parse() shouldBe
            PostedNotification.Unsnoozable(
                key,
                Origin(SLACK, Channel.Named(ChannelId("messages")), Conversation.None),
                traits(trait),
            )
    }

    @Test
    fun `every blocking trait is kept`() {
        val traits = traits(NotificationTrait.Ongoing, NotificationTrait.ForegroundService, NotificationTrait.NoClear)

        snapshot(SLACK, "media", traits.values).parse() shouldBe
            PostedNotification.Unsnoozable(
                key,
                Origin(SLACK, Channel.Named(ChannelId("media")), Conversation.None),
                traits,
            )
    }

    private fun snapshot(
        packageName: PackageName,
        channelId: String?,
        traits: Set<NotificationTrait> = emptySet(),
        shortcutId: String? = null,
    ): NotificationSnapshot = NotificationSnapshot(key, packageName, channelId, shortcutId, traits)
}
