package dev.danielgilchrist.knockoff.domain.channel

import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId
import dev.danielgilchrist.knockoff.domain.shouldBe
import org.junit.jupiter.api.Test

class ChannelGroupingTest {
    @Test
    fun `a channel with a group id is grouped`() {
        ChannelGrouping.of("^ncg_1_account_dan@work.example") shouldBe
            ChannelGrouping.Grouped(ChannelGroupId("^ncg_1_account_dan@work.example"))
    }

    @Test
    fun `a channel without a group is ungrouped`() {
        ChannelGrouping.of(null) shouldBe ChannelGrouping.Ungrouped
        ChannelGrouping.of("") shouldBe ChannelGrouping.Ungrouped
        ChannelGrouping.of(" ") shouldBe ChannelGrouping.Ungrouped
    }
}
