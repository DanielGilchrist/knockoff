package dev.danielgilchrist.knockoff.domain.notification

import dev.danielgilchrist.knockoff.domain.WORK_ADDRESS
import dev.danielgilchrist.knockoff.domain.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ChannelIdTest {
    @ParameterizedTest(name = "{0} belongs to the work account: {1}")
    @CsvSource(
        "^nc_1_mail_dan@work.example, true",
        "^nc_2_Receipts_dan@work.example, true",
        "^nc_1_mail_Dan@Work.Example, true",
        "^nc_1_mail_dan@home.example, false",
        "^nc_1_mail_otherdan@work.example, false",
        "^nc_1_mail_dan@work.example.evil, false",
        "dan@work.example, false",
        "^nc_~_misc, false",
    )
    fun `channel ends with the account after an underscore`(
        channel: String,
        belongs: Boolean,
    ) {
        ChannelId(channel).endsWithAccount(WORK_ADDRESS) shouldBe belongs
    }
}
