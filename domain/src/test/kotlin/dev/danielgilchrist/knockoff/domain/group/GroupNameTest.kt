package dev.danielgilchrist.knockoff.domain.group

import dev.danielgilchrist.knockoff.domain.address
import dev.danielgilchrist.knockoff.domain.groupName
import dev.danielgilchrist.knockoff.domain.shouldBe
import org.junit.jupiter.api.Test

class GroupNameTest {
    private val work = address("dan@work.example")

    @Test
    fun `a group names an address it spells, whatever the case`() {
        groupName("dan@work.example").names(work) shouldBe true
        groupName("Dan@Work.Example").names(work) shouldBe true
    }

    @Test
    fun `a group doesn't name a different address`() {
        groupName("Work").names(work) shouldBe false
        groupName("dan@work.example.org").names(work) shouldBe false
    }
}
