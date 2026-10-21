package dev.danielgilchrist.knockoff.domain.group

import dev.danielgilchrist.knockoff.domain.channel.ChannelGrouping
import dev.danielgilchrist.knockoff.domain.channel.ChannelKey
import dev.danielgilchrist.knockoff.domain.notification.Channel
import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.Conversation
import dev.danielgilchrist.knockoff.domain.notification.Origin
import dev.danielgilchrist.knockoff.domain.notification.OriginGroup
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class KnownGroupsTest {
    private val gmail = PackageName("com.google.android.gm")
    private val work = ChannelGroupId("^ncg_1_account_dan@work.example")
    private val home = ChannelGroupId("^ncg_1_account_dan@home.example")
    private val workMail = ChannelKey(gmail, ChannelId("^nc_1_mail_dan@work.example"))
    private val homeMail = ChannelKey(gmail, ChannelId("^nc_1_mail_dan@home.example"))

    private fun name(raw: String): GroupName = GroupName.parse(raw).shouldBeInstanceOf<GroupNameParse.Valid>().name

    @Test
    fun `a sighted channel's group is known`() {
        val known = KnownGroups.NONE.sighted(workMail, ChannelGrouping.Grouped(work))

        known.groupOf(workMail) shouldBe OriginGroup.Known(ChannelGrouping.Grouped(work))
        known.groupOf(homeMail) shouldBe OriginGroup.Unknown
        known.groupsOf(gmail) shouldBe listOf(KnownGroup(work, GroupLabel.Unnamed))
    }

    @Test
    fun `sighting a channel already known changes nothing, not even the order`() {
        val known =
            KnownGroups.NONE
                .sighted(workMail, ChannelGrouping.Grouped(work))
                .sighted(ChannelKey(PackageName("com.whatsapp"), ChannelId("chats")), ChannelGrouping.Grouped(home))

        known.sighted(workMail, ChannelGrouping.Grouped(work)) shouldBe known
        known
            .sighted(workMail, ChannelGrouping.Grouped(home))
            .apps
            .first()
            .packageName shouldBe gmail
    }

    @Test
    fun `a posted notification's channel group is learned when known`() {
        val known = OriginGroup.Known(ChannelGrouping.Grouped(work))
        val named = Channel.Named(workMail.channelId)

        KnownGroups.NONE.sighted(Origin(gmail, named, Conversation.None, known)).groupOf(workMail) shouldBe known
        KnownGroups.NONE.sighted(Origin(gmail, named, Conversation.None, OriginGroup.Unknown)) shouldBe KnownGroups.NONE
        KnownGroups.NONE.sighted(Origin(gmail, Channel.Unnamed, Conversation.None, known)) shouldBe KnownGroups.NONE
    }

    @Test
    fun `an ungrouped sighting changes nothing`() {
        KnownGroups.NONE.sighted(workMail, ChannelGrouping.Ungrouped) shouldBe KnownGroups.NONE
    }

    @Test
    fun `a read adds to the app's channels and replaces its group names`() {
        val known =
            KnownGroups.NONE
                .read(gmail, emptyMap(), mapOf(work to name("dan@work.example")))
                .sighted(workMail, ChannelGrouping.Grouped(work))
                .read(gmail, mapOf(homeMail.channelId to home), mapOf(home to name("dan@home.example")))

        known.groupOf(workMail) shouldBe OriginGroup.Known(ChannelGrouping.Grouped(work))
        known.groupOf(homeMail) shouldBe OriginGroup.Known(ChannelGrouping.Grouped(home))
        known.apps.single().names shouldBe mapOf(home to name("dan@home.example"))
    }

    @Test
    fun `reading apps that are already known changes nothing, in any order`() {
        val whatsapp = PackageName("com.whatsapp")
        val chats = mapOf(ChannelId("chats") to home)
        val known =
            KnownGroups.NONE
                .read(gmail, mapOf(workMail.channelId to work), mapOf(work to name("dan@work.example")))
                .read(whatsapp, chats, emptyMap())

        known.read(gmail, mapOf(workMail.channelId to work), mapOf(work to name("dan@work.example"))) shouldBe known
        known.read(whatsapp, chats, emptyMap()) shouldBe known
    }

    @Test
    fun `a read that names a group lists it named`() {
        val known =
            KnownGroups.NONE.read(gmail, mapOf(homeMail.channelId to home), mapOf(home to name("dan@home.example")))

        known.groupsOf(gmail) shouldBe listOf(KnownGroup(home, GroupLabel.Named(name("dan@home.example"))))
        known.groupsOf(gmail).single().id shouldBe home
        known
            .groupsOf(gmail)
            .single()
            .label
            .shouldBeInstanceOf<GroupLabel.Named>()
            .name.value shouldBe
            "dan@home.example"
    }

    @Test
    fun `a named group without channels is still listed`() {
        KnownGroups.NONE.read(gmail, emptyMap(), mapOf(work to name("dan@work.example"))).groupsOf(gmail) shouldBe
            listOf(KnownGroup(work, GroupLabel.Named(name("dan@work.example"))))
    }

    @Test
    fun `the latest app comes first and only the latest are kept`() {
        val many =
            (0..KnownGroups.LIMIT).fold(KnownGroups.NONE) { known, index ->
                known.sighted(ChannelKey(PackageName("app$index"), ChannelId("c")), ChannelGrouping.Grouped(work))
            }

        many.apps.size shouldBe KnownGroups.LIMIT
        many.apps.first().packageName shouldBe PackageName("app${KnownGroups.LIMIT}")
        many.groupOf(ChannelKey(PackageName("app0"), ChannelId("c"))) shouldBe OriginGroup.Unknown
    }

    @Test
    fun `sighting another channel keeps the app's other channels`() {
        val known =
            KnownGroups.NONE
                .sighted(workMail, ChannelGrouping.Grouped(work))
                .sighted(homeMail, ChannelGrouping.Grouped(home))

        known.apps.size shouldBe 1
        known.groupOf(workMail) shouldBe OriginGroup.Known(ChannelGrouping.Grouped(work))
    }

    @Test
    fun `known groups round-trip`() {
        val known =
            KnownGroups.NONE
                .read(gmail, mapOf(workMail.channelId to work), mapOf(work to name("dan@work.example")))
                .sighted(ChannelKey(PackageName("com.whatsapp"), ChannelId("chats")), ChannelGrouping.Grouped(home))

        KnownGroups.fromJson(known.toJson()) shouldBe KnownGroupsDecode.Decoded(known)
    }

    @Test
    fun `blank stored names are dropped`() {
        val text = """{"version":1,"apps":[{"app":"a","channels":{"c":"g"},"names":{"g":" "}}]}"""

        KnownGroups
            .fromJson(
                text,
            ).shouldBeInstanceOf<KnownGroupsDecode.Decoded>()
            .groups
            .groupsOf(PackageName("a")) shouldBe
            listOf(KnownGroup(ChannelGroupId("g"), GroupLabel.Unnamed))
    }

    @Test
    fun `other versions and unreadable text fail`() {
        KnownGroups.fromJson("""{"version":2,"apps":[]}""") shouldBe KnownGroupsDecode.UnsupportedVersion(2)
        KnownGroups
            .fromJson("nope")
            .shouldBeInstanceOf<KnownGroupsDecode.Unreadable>()
            .detail
            .isNotBlank() shouldBe
            true
        KnownGroupsDecode.UnsupportedVersion(2).version shouldBe 2
        KnownGroupsDecode.UnsupportedVersion(2).detail shouldBe "Unsupported known groups version 2"
    }

    @Test
    fun `group names are trimmed and blank ones refused`() {
        GroupName.parse("  dan@work.example ") shouldBe GroupNameParse.Valid(name("dan@work.example"))
        GroupName.parse(" ") shouldBe GroupNameParse.Blank
    }
}
