package dev.danielgilchrist.knockoff.domain.notification

import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.shouldBeInstanceOf
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class AccountAddressTest {
    @ParameterizedTest
    @ValueSource(strings = ["me@work.example", "  Me@Work.Example ", "ME@WORK.EXAMPLE"])
    fun `valid addresses are trimmed and lower-cased`(raw: String) {
        AccountAddress
            .parse(raw)
            .shouldBeInstanceOf<AccountAddressParse.Valid>()
            .address.value shouldBe
            "me@work.example"
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "   ", "me", "me@", "@work.example", "me@work", "me@@work.example", "m e@work.example"])
    fun `invalid addresses explain themselves`(raw: String) {
        AccountAddress.parse(raw).shouldBeInstanceOf<AccountAddressParse.Invalid>().detail shouldBe
            "Not an email address: \"$raw\""
    }
}
