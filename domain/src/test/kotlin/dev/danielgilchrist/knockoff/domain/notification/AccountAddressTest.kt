package dev.danielgilchrist.knockoff.domain.notification

import dev.danielgilchrist.knockoff.domain.address
import dev.danielgilchrist.knockoff.domain.group.GroupName
import dev.danielgilchrist.knockoff.domain.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class AccountAddressTest {
    @Test
    fun `an address is trimmed and lowercased`() {
        address("  Dan@Work.Example ").value shouldBe "dan@work.example"
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(
        strings = ["", " ", "2 new messages", "@dan", "dan@", "dan@work", "dan smith@work.example", "a@@work.example"],
    )
    fun `text that isn't shaped like an address is not an address`(raw: String) {
        AccountAddress.parse(raw) shouldBe AccountAddressParse.NotAnAddress
    }

    @Test
    fun `an address names its account`() {
        GroupName.of(address("dan@work.example")).value shouldBe "dan@work.example"
    }
}
