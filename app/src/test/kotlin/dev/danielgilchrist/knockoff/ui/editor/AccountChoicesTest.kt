package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.address
import dev.danielgilchrist.knockoff.domain.group.GroupLabel
import dev.danielgilchrist.knockoff.domain.group.GroupName
import dev.danielgilchrist.knockoff.domain.group.KnownGroup
import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId
import dev.danielgilchrist.knockoff.domain.rule.AccountId
import dev.danielgilchrist.knockoff.groupName
import dev.danielgilchrist.knockoff.ui.GroupNoun
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AccountChoicesTest {
    private val narrowed = AppDraft.EVERYTHING.copy(mode = AppMode.Narrowed)
    private val chats = KnownGroup(ChannelGroupId("chats"), GroupLabel.Named(groupName("Chats")))
    private val calls = KnownGroup(ChannelGroupId("calls"), GroupLabel.Named(groupName("Calls")))
    private val work = address("dan@work.example")
    private val workId = AccountId.Address(work)

    @Test
    fun `learning an address keeps an app's categories and lists it as an account beside them`() {
        val choices = AccountChoices.of(listOf(chats, calls), listOf(work), emptyMap(), narrowed)

        assertEquals(
            AccountList.Split(
                categories =
                    mapOf(
                        AccountId.Group(chats.id) to groupName("Chats"),
                        AccountId.Group(calls.id) to groupName("Calls"),
                    ),
                accounts = mapOf(workId to GroupName.of(work)),
            ),
            choices.list,
        )
        assertEquals(GroupNoun.AccountsAndCategories, choices.noun)
        assertEquals(GroupNoun.Categories, choices.list.groupNoun)
        assertEquals("1 account and 2 categories", choices.list.choosing)
    }

    @Test
    fun `a list to choose from counts each kind and drops the empty ones`() {
        val accountsOnly =
            AccountChoices.of(
                emptyList(),
                listOf(work, address("dan@home.example")),
                emptyMap(),
                narrowed,
            )
        val categoriesOnly = AccountChoices.of(listOf(chats), emptyList(), emptyMap(), narrowed)

        assertEquals("2 accounts", accountsOnly.list.choosing)
        assertEquals("1 category", categoriesOnly.list.choosing)
    }

    @Test
    fun `addresses alone, or beside account groups, make one list of accounts`() {
        val gmailWork =
            KnownGroup(
                ChannelGroupId("^ncg_1_account_dan@home.example"),
                GroupLabel.Named(groupName("dan@home.example")),
            )

        assertEquals(
            AccountList.Single(GroupNoun.Accounts, mapOf(workId to GroupName.of(work))),
            AccountChoices.of(emptyList(), listOf(work), emptyMap(), narrowed).list,
        )
        assertEquals(
            AccountList.Single(
                GroupNoun.Accounts,
                mapOf(AccountId.Group(gmailWork.id) to groupName("dan@home.example"), workId to GroupName.of(work)),
            ),
            AccountChoices.of(listOf(gmailWork), listOf(work), emptyMap(), narrowed).list,
        )
        assertEquals(
            AccountList.Single(GroupNoun.Categories, mapOf(AccountId.Group(chats.id) to groupName("Chats"))),
            AccountChoices.of(listOf(chats), emptyList(), emptyMap(), narrowed).list,
        )
    }

    @Test
    fun `a stored address a group already names is shown once, unless it is still picked`() {
        val group =
            KnownGroup(
                ChannelGroupId("^ncg_1_account_dan@work.example"),
                GroupLabel.Named(groupName("Dan@Work.Example")),
            )
        val stored = mapOf<AccountId, GroupName>(workId to GroupName.of(work))

        assertEquals(
            mapOf(AccountId.Group(group.id) to groupName("Dan@Work.Example")),
            AccountChoices.of(listOf(group), listOf(work), stored, narrowed).offered,
        )
        assertEquals(
            mapOf(AccountId.Group(group.id) to groupName("Dan@Work.Example"), workId to GroupName.of(work)),
            AccountChoices.of(listOf(group), listOf(work), stored, narrowed.copy(accounts = stored)).offered,
        )
    }

    @Test
    fun `a typed address is picked and narrows the app`() {
        val typed = AppDraft.EVERYTHING.withAddress(work)

        assertEquals(AppMode.Narrowed, typed.mode)
        assertEquals(mapOf(workId to GroupName.of(work)), typed.accounts)
        assertEquals(listOf(work), typed.pickedAddresses)
        assertEquals(
            emptyList<Any>(),
            narrowed.copy(accounts = mapOf(AccountId.Group(chats.id) to groupName("Chats"))).pickedAddresses,
        )
    }

    @Test
    fun `picked addresses warn that their notifications may buzz once`() {
        assertEquals(
            "Proton Mail only says which account a notification is for once it arrives, so notifications to " +
                "dan@work.example and dan@home.example may buzz once before they're held.",
            buzzCaption("Proton Mail", work, listOf(address("dan@home.example"))),
        )
    }

    @Test
    fun `a summary counts categories and accounts apart when they are listed apart`() {
        val home = address("dan@home.example")
        val choices = AccountChoices.of(listOf(chats, calls), listOf(work, home), emptyMap(), narrowed)
        val bothAddresses =
            narrowed.copy(
                accounts =
                    mapOf(
                        workId to GroupName.of(work),
                        AccountId.Address(home) to GroupName.of(home),
                    ),
            )
        val mixed =
            narrowed.copy(
                accounts =
                    mapOf(
                        workId to GroupName.of(work),
                        AccountId.Group(chats.id) to groupName("Chats"),
                    ),
            )

        assertEquals("2 accounts", AppSummary.of(bothAddresses, choices))
        assertEquals("1 account · 1 category", AppSummary.of(mixed, choices))
        assertEquals(
            "2 accounts",
            AppSummary.of(bothAddresses, AccountChoices.of(emptyList(), listOf(work, home), emptyMap(), narrowed)),
        )
    }
}
