package dev.danielgilchrist.knockoff.domain.channel

import dev.danielgilchrist.knockoff.domain.SLACK
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.shouldBe
import dev.danielgilchrist.knockoff.domain.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class SilencedChannelsTest {
    private val messages = ChannelKey(SLACK, ChannelId("messages"))
    private val mail = ChannelKey(PackageName.GMAIL, ChannelId("^nc_1_mail_dan@work.example"))
    private val silenced =
        SilencedChannels.of(
            mapOf(
                messages to AlertingImportance.High,
                mail to AlertingImportance.Default,
            ),
        )

    @Test
    fun `silenced channels round-trip through JSON`() {
        SilencedChannels.fromJson(silenced.toJson()) shouldBe SilencedChannelsDecode.Decoded(silenced)
        SilencedChannels.fromJson(SilencedChannels.NONE.toJson()) shouldBe
            SilencedChannelsDecode.Decoded(SilencedChannels.NONE)
    }

    @Test
    fun `the stored document is versioned and names every field`() {
        SilencedChannels.of(mapOf(messages to AlertingImportance.High)).toJson() shouldBe
            """
            {
                "version": 1,
                "channels": [
                    {
                        "package": "com.Slack",
                        "channel": "messages",
                        "original": "High"
                    }
                ]
            }
            """.trimIndent()
    }

    @Test
    fun `silenced packages are those of every saved channel`() {
        silenced.packages shouldBe setOf(SLACK, PackageName.GMAIL)
    }

    @Test
    fun `envelope failures keep their detail`() {
        SilencedChannels.fromJson("[]").shouldBeInstanceOf<SilencedChannelsDecode.Unreadable>().detail shouldBe
            "not a JSON object"

        val unsupported = SilencedChannels.fromJson("""{"version": 2, "channels": []}""")

        unsupported.shouldBeInstanceOf<SilencedChannelsDecode.UnsupportedVersion>().version shouldBe 2
        unsupported.shouldBeInstanceOf<SilencedChannelsDecode.Failure>().detail shouldBe
            "Unsupported silenced channels version 2"
    }

    @Test
    fun `an unknown or quiet original is unreadable`() {
        listOf("Loud", "Low", "Min", "None").forEach { original ->
            SilencedChannels
                .fromJson("""{"version": 1, "channels": [{"package": "a", "channel": "b", "original": "$original"}]}""")
                .shouldBeInstanceOf<SilencedChannelsDecode.Unreadable>()
        }
    }

    @Test
    fun `a channel saved twice is refused`() {
        val decode =
            SilencedChannels.fromJson(
                """
                {"version": 1, "channels": [
                    {"package": "com.Slack", "channel": "messages", "original": "High"},
                    {"package": "com.Slack", "channel": "messages", "original": "Default"}
                ]}
                """.trimIndent(),
            )

        decode.shouldBeInstanceOf<SilencedChannelsDecode.DuplicateChannels>().keys shouldBe setOf(messages)
        decode.shouldBeInstanceOf<SilencedChannelsDecode.Failure>().detail shouldBe
            "Channels saved more than once: [com.Slack/messages]"
    }
}
