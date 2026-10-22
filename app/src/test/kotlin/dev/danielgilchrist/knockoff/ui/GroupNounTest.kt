package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.groupName
import dev.danielgilchrist.knockoff.ui.editor.AppKind
import dev.danielgilchrist.knockoff.ui.editor.AppSummary
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GroupNounTest {
    @Test
    fun `a group reads as an account only when its name is an address`() {
        assertEquals(GroupNoun.Accounts, GroupNoun.of(listOf(groupName("Chats"), groupName("dan@work.example"))))
        assertEquals(GroupNoun.Categories, GroupNoun.of(listOf(groupName("dan@work"), groupName("@team"))))
    }

    @Test
    fun `categories and accounts together read as both`() {
        val both = GroupNoun.AccountsAndCategories

        assertEquals("Only some accounts and categories", AppKind.Accounts.narrowed(both))
        assertEquals("Only some accounts, categories and chats", AppKind.AccountsAndChats.narrowed(both))
        assertEquals("account, category or chat", AppKind.AccountsAndChats.pick(both))
        assertEquals("No accounts, categories or chats picked", AppSummary.unpicked(AppKind.AccountsAndChats, both))
        assertEquals("No accounts or categories picked", AppSummary.unpicked(AppKind.Accounts, both))
        assertEquals("No accounts picked", AppSummary.unpicked(AppKind.Accounts, GroupNoun.Accounts))
        assertEquals("2 accounts and categories", both.counted(2))
        assertEquals("1 account or category", both.counted(1))
    }
}
