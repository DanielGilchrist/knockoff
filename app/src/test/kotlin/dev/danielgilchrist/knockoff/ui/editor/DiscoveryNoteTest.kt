package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.domain.group.GroupLabel
import dev.danielgilchrist.knockoff.domain.group.KnownGroup
import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId
import dev.danielgilchrist.knockoff.group.GroupDiscovery
import dev.danielgilchrist.knockoff.groupName
import dev.danielgilchrist.knockoff.ui.AppDiscovery
import dev.danielgilchrist.knockoff.ui.DeviceLink
import dev.danielgilchrist.knockoff.ui.LinkState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DiscoveryNoteTest {
    private val linked = LinkState.Known(DeviceLink.Linked("Watch"))
    private val unlinked = LinkState.Known(DeviceLink.NotLinked)
    private val notLinked = AppDiscovery.Finished(GroupDiscovery.NotLinked("no association"))
    private val misc = KnownGroup(ChannelGroupId("misc"), GroupLabel.Unnamed)
    private val work = KnownGroup(ChannelGroupId("work"), GroupLabel.Named(groupName("dan@work.example")))
    private val nothing = facts("Maps", AppKind.Plain, emptyList())

    @Test
    fun `nothing is said about looking once an app offers accounts or categories`() {
        val gmail = facts("Gmail", AppKind.Accounts, listOf(work))

        listOf(AppDiscovery.NotAsked, AppDiscovery.Reading, notLinked, AppDiscovery.Finished(GroupDiscovery.Unbound))
            .forEach { assertEquals(DiscoveryNote.None, DiscoveryNote.of(gmail, it, unlinked)) }
    }

    @Test
    fun `an app with nothing offered says how looking went`() {
        assertEquals(DiscoveryNote.None, DiscoveryNote.of(nothing, AppDiscovery.NotAsked, linked))
        assertEquals(
            DiscoveryNote.Text("Checking Maps for accounts and categories…"),
            DiscoveryNote.of(nothing, AppDiscovery.Reading, linked),
        )
        assertEquals(
            DiscoveryNote.Text(
                "Maps has no accounts or categories to pick from. If it has chats, they'll show here after it " +
                    "notifies you.",
            ),
            DiscoveryNote.of(nothing, AppDiscovery.Finished(GroupDiscovery.Read), linked),
        )
        assertEquals(
            DiscoveryNote.None,
            DiscoveryNote.of(nothing.copy(kind = AppKind.Chats), AppDiscovery.Finished(GroupDiscovery.Read), linked),
        )
        assertEquals(DiscoveryNote.None, DiscoveryNote.of(nothing, notLinked, unlinked))
        assertEquals(
            DiscoveryNote.Text(
                "Knockoff couldn't look for Maps' accounts and categories because it isn't connected to your " +
                    "notifications right now.",
            ),
            DiscoveryNote.of(nothing, AppDiscovery.Finished(GroupDiscovery.Unbound), linked),
        )
        assertEquals(
            DiscoveryNote.Text("Knockoff found Maps' accounts and categories but couldn't save them: disk full"),
            DiscoveryNote.of(nothing, AppDiscovery.Finished(GroupDiscovery.Unrecorded("disk full")), linked),
        )
    }

    @Test
    fun `categories known only by id ask for the link whatever is offered`() {
        val slack = facts("Slack", AppKind.Accounts, listOf(misc))
        val read = AppDiscovery.Finished(GroupDiscovery.Read)

        assertEquals(
            DiscoveryNote.LinkNeeded(
                "Link a device to see category names",
                "Knockoff knows some of Slack's categories only by ID, because reading their names needs the " +
                    "device link. Set it up in Settings, under Silent holds.",
            ),
            DiscoveryNote.of(slack, read, unlinked),
        )
        assertEquals(DiscoveryNote.None, DiscoveryNote.of(slack, read, linked))
        assertEquals(DiscoveryNote.None, DiscoveryNote.of(slack, read, LinkState.Loading))
    }

    private fun facts(
        label: String,
        kind: AppKind,
        known: List<KnownGroup>,
    ): DiscoveryFacts =
        DiscoveryFacts(label, kind, AccountChoices.of(known, emptyList(), emptyMap(), AppDraft.EVERYTHING))
}
