package dev.danielgilchrist.knockoff.domain.notification

import dev.danielgilchrist.knockoff.domain.COLLEAGUE
import dev.danielgilchrist.knockoff.domain.SLACK
import dev.danielgilchrist.knockoff.domain.WHATSAPP
import dev.danielgilchrist.knockoff.domain.WHATSAPP_CHATS
import dev.danielgilchrist.knockoff.domain.address
import dev.danielgilchrist.knockoff.domain.channel.ChannelGrouping
import dev.danielgilchrist.knockoff.domain.known
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
            PostedNotification.Snoozable(
                key,
                Origin(
                    SLACK,
                    Channel.Named(ChannelId("messages")),
                    Conversation.None,
                    OriginGroup.Unknown,
                    OriginAddress.Unaddressed,
                    NotificationCategory.Other,
                ),
            )
    }

    @Test
    fun `a gmail notification keeps its account channel`() {
        snapshot(PackageName.GMAIL, "^nc_1_mail_dan@work.example").parse() shouldBe
            PostedNotification.Snoozable(
                key,
                Origin(
                    PackageName.GMAIL,
                    Channel.Named(ChannelId("^nc_1_mail_dan@work.example")),
                    Conversation.None,
                    OriginGroup.Unknown,
                    OriginAddress.Unaddressed,
                    NotificationCategory.Other,
                ),
            )
    }

    @Test
    fun `a notification keeps the group its channel is in`() {
        val grouped = OriginGroup.Known(ChannelGrouping.Grouped(ChannelGroupId("^ncg_1_account_dan@work.example")))

        snapshot(PackageName.GMAIL, "^nc_1_mail_dan@work.example", group = grouped).parse().origin.group shouldBe
            grouped
    }

    @Test
    fun `a notification without a channel is unnamed`() {
        snapshot(SLACK, channelId = null).parse() shouldBe
            PostedNotification.Snoozable(
                key,
                Origin(
                    SLACK,
                    Channel.Unnamed,
                    Conversation.None,
                    OriginGroup.Unknown,
                    OriginAddress.Unaddressed,
                    NotificationCategory.Other,
                ),
            )
    }

    @Test
    fun `a messaging notification with a shortcut belongs to that conversation`() {
        snapshot(WHATSAPP, WHATSAPP_CHATS.value, shortcutId = COLLEAGUE.value).parse() shouldBe
            PostedNotification.Snoozable(key, whatsappOrigin(known(COLLEAGUE)))
    }

    @Test
    fun `a conversation is titled from the first usable title`() {
        snapshot(
            WHATSAPP,
            WHATSAPP_CHATS.value,
            shortcutId = COLLEAGUE.value,
            titles = listOf(" ", " Boss ", "Boss (2)"),
        ).parse()
            .origin.conversation shouldBe known(COLLEAGUE, "Boss")
    }

    @Test
    fun `a conversation without a usable title is untitled`() {
        snapshot(WHATSAPP, WHATSAPP_CHATS.value, shortcutId = COLLEAGUE.value, titles = listOf("", " "))
            .parse()
            .origin.conversation shouldBe Conversation.Known(COLLEAGUE, ConversationTitle.Untitled)
    }

    @Test
    fun `a notification that isn't a messaging one is only routed by its shortcut`() {
        snapshot(WHATSAPP, WHATSAPP_CHATS.value, shortcutId = COLLEAGUE.value, style = NotificationStyle.Other)
            .parse()
            .origin.conversation shouldBe Conversation.Routed(COLLEAGUE)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " "])
    fun `a blank shortcut is no conversation`(shortcutId: String) {
        snapshot(WHATSAPP, WHATSAPP_CHATS.value, shortcutId = shortcutId).parse().origin.conversation shouldBe
            Conversation.None
        snapshot(WHATSAPP, WHATSAPP_CHATS.value, shortcutId = shortcutId, style = NotificationStyle.Other)
            .parse()
            .origin.conversation shouldBe Conversation.None
    }

    @ParameterizedTest
    @EnumSource(NotificationTrait::class)
    fun `any blocking trait makes it unsnoozable`(trait: NotificationTrait) {
        snapshot(SLACK, "messages", setOf(trait)).parse() shouldBe
            PostedNotification.Unsnoozable(
                key,
                Origin(
                    SLACK,
                    Channel.Named(ChannelId("messages")),
                    Conversation.None,
                    OriginGroup.Unknown,
                    OriginAddress.Unaddressed,
                    NotificationCategory.Other,
                ),
                traits(trait),
            )
    }

    @Test
    fun `every blocking trait is kept`() {
        val traits = traits(NotificationTrait.Ongoing, NotificationTrait.ForegroundService, NotificationTrait.NoClear)

        snapshot(SLACK, "media", traits.values).parse() shouldBe
            PostedNotification.Unsnoozable(
                key,
                Origin(
                    SLACK,
                    Channel.Named(ChannelId("media")),
                    Conversation.None,
                    OriginGroup.Unknown,
                    OriginAddress.Unaddressed,
                    NotificationCategory.Other,
                ),
                traits,
            )
    }

    @Test
    fun `an address in the sub text is carried on the origin`() {
        snapshot(SLACK, "messages", subText = " Dan@Work.Example ").parse().origin.address shouldBe
            OriginAddress.Addressed(address("dan@work.example"))
        snapshot(SLACK, "messages", subText = "#general").parse().origin.address shouldBe OriginAddress.Unaddressed
        snapshot(SLACK, "messages", category = NotificationCategory.Email).parse().origin.category shouldBe
            NotificationCategory.Email
    }

    private fun snapshot(
        packageName: PackageName,
        channelId: String?,
        traits: Set<NotificationTrait> = emptySet(),
        shortcutId: String? = null,
        titles: List<String> = listOf("Colleague"),
        style: NotificationStyle = NotificationStyle.Messaging,
        group: OriginGroup = OriginGroup.Unknown,
        subText: String? = null,
        category: NotificationCategory = NotificationCategory.Other,
    ): NotificationSnapshot =
        NotificationSnapshot(key, packageName, channelId, group, subText, category, style, shortcutId, titles, traits)
}
