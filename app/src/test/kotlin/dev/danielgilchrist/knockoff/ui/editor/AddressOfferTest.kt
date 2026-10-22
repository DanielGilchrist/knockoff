package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.ui.AppPurpose
import dev.danielgilchrist.knockoff.ui.GroupNoun
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AddressOfferTest {
    private val narrowed = AppDraft.EVERYTHING.copy(mode = AppMode.Narrowed)

    @Test
    fun `a narrowed mail app can always take a typed account`() {
        GroupNoun.entries.forEach { noun ->
            assertEquals(AddressOffer.Shown, AddressOffer.of(narrowed, 0, AppPurpose.Mail, noun))
            assertEquals(AddressOffer.Shown, AddressOffer.of(narrowed, 3, AppPurpose.Mail, noun))
        }
    }

    @Test
    fun `a narrowed other app takes a typed account only once it has accounts`() {
        assertEquals(AddressOffer.Shown, AddressOffer.of(narrowed, 1, AppPurpose.Other, GroupNoun.Accounts))
        assertEquals(
            AddressOffer.Shown,
            AddressOffer.of(narrowed, 2, AppPurpose.Other, GroupNoun.AccountsAndCategories),
        )
        assertEquals(AddressOffer.Hidden, AddressOffer.of(narrowed, 0, AppPurpose.Other, GroupNoun.Categories))
        assertEquals(AddressOffer.Hidden, AddressOffer.of(narrowed, 3, AppPurpose.Other, GroupNoun.Categories))
    }

    @Test
    fun `an app held whole offers typing only when it is a mail app with nothing else to narrow by`() {
        val whole = AppDraft.EVERYTHING

        assertEquals(AddressOffer.Shown, AddressOffer.of(whole, 0, AppPurpose.Mail, GroupNoun.Categories))
        assertEquals(AddressOffer.Hidden, AddressOffer.of(whole, 0, AppPurpose.Other, GroupNoun.Categories))
        assertEquals(AddressOffer.Hidden, AddressOffer.of(whole, 0, AppPurpose.Other, GroupNoun.Accounts))
        assertEquals(AddressOffer.Hidden, AddressOffer.of(whole, 1, AppPurpose.Mail, GroupNoun.Accounts))
    }
}
