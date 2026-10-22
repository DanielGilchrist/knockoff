package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.address
import dev.danielgilchrist.knockoff.domain.group.GroupName
import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId
import dev.danielgilchrist.knockoff.domain.rule.AccountId
import dev.danielgilchrist.knockoff.groupName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AddressFieldTest {
    private val work = address("dan@work.example")
    private val workId = AccountId.Address(work)
    private val workGroup = AccountId.Group(ChannelGroupId("work"))
    private val chats = AccountId.Group(ChannelGroupId("chats"))

    @Test
    fun `a new address is added, trimmed and lowercased`() {
        assertEquals(AddressSubmission.Added(work), open(" Dan@Work.example ").submitted(emptyMap(), emptySet()))
    }

    @Test
    fun `text that isn't an address is invalid`() {
        listOf("", "dan", "dan@work", "dan @work.example").forEach { text ->
            assertEquals(AddressSubmission.Invalid, open(text).submitted(emptyMap(), emptySet()))
        }
    }

    @Test
    fun `an address already ticked is a duplicate`() {
        val offered = mapOf<AccountId, GroupName>(workId to GroupName.of(work))

        assertEquals(AddressSubmission.Duplicate(work), open("DAN@work.example").submitted(offered, setOf(workId)))
    }

    @Test
    fun `an address offered but not ticked is ticked`() {
        val offered = mapOf<AccountId, GroupName>(workId to GroupName.of(work))

        assertEquals(AddressSubmission.TicksListed(work), open("DAN@work.example").submitted(offered, setOf(chats)))
    }

    @Test
    fun `an address naming an offered group ticks that group, whatever its case`() {
        val offered =
            mapOf<AccountId, GroupName>(
                chats to groupName("Chats"),
                workGroup to groupName("Dan@Work.example"),
            )

        assertEquals(
            AddressSubmission.MatchesGroup(workGroup, groupName("Dan@Work.example")),
            open("dan@work.example").submitted(offered, emptySet()),
        )
    }

    @Test
    fun `answering closes the field on success and shows the problem otherwise`() {
        val field = open("dan@work.example")

        assertEquals(AddressField.Closed, field.answered(AddressSubmission.Added(work)))
        assertEquals(AddressField.Closed, field.answered(AddressSubmission.TicksListed(work)))
        assertEquals(AddressField.Closed, field.answered(AddressSubmission.MatchesGroup(chats, groupName("Chats"))))
        assertEquals(
            AddressField.Open("dan@work.example", AddressProblem.NotAnAddress),
            field.answered(AddressSubmission.Invalid),
        )
        assertEquals(
            AddressField.Open("dan@work.example", AddressProblem.AlreadyListed(work)),
            field.answered(AddressSubmission.Duplicate(work)),
        )
    }

    @Test
    fun `editing clears the problem`() {
        assertEquals(
            AddressField.Open("dan@", AddressProblem.None),
            AddressField.Open("dan", AddressProblem.NotAnAddress).edited("dan@"),
        )
    }

    @Test
    fun `a duplicate names the address`() {
        assertEquals("dan@work.example is already listed.", AddressProblem.AlreadyListed(work).message)
    }

    private fun open(text: String): AddressField.Open = AddressField.Open(text, AddressProblem.None)
}
