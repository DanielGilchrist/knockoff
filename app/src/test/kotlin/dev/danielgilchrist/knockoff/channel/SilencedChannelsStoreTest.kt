package dev.danielgilchrist.knockoff.channel

import dev.danielgilchrist.knockoff.domain.channel.ChannelGrouping
import dev.danielgilchrist.knockoff.domain.channel.ChannelKey
import dev.danielgilchrist.knockoff.domain.channel.ChannelKind
import dev.danielgilchrist.knockoff.domain.channel.Importance
import dev.danielgilchrist.knockoff.domain.channel.ReadChannel
import dev.danielgilchrist.knockoff.domain.channel.SilencedChannels
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.domain.rule.RuleApps
import dev.danielgilchrist.knockoff.domain.rule.RuleSetParse
import dev.danielgilchrist.knockoff.domain.rule.Target
import dev.danielgilchrist.knockoff.domain.settings.Settings
import dev.danielgilchrist.knockoff.ruleApps
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.ZonedDateTime

class SilencedChannelsStoreTest {
    @TempDir
    lateinit var directory: File

    private val slack = PackageName("com.Slack")
    private val messages = ChannelKey(slack, ChannelId("messages"))

    @Test
    fun `a missing file means nothing is silenced`() =
        runTest {
            assertEquals(SilencedChannelsLoad.Loaded(SilencedChannels.NONE), store().load())
        }

    @Test
    fun `saved channels load back`() =
        runTest {
            val silenced = silenced()

            assertEquals(SilencedChannelsSave.Saved, store().save(silenced))
            assertEquals(SilencedChannelsLoad.Loaded(silenced), store().load())
        }

    @Test
    fun `an undecodable file fails to load`() =
        runTest {
            file().writeBytes(byteArrayOf(0xC3.toByte()))

            assertInstanceOf(SilencedChannelsLoad.Failed::class.java, store().load())
        }

    @Test
    fun `an unsupported version fails to load with its detail`() =
        runTest {
            file().writeText("""{"version": 2, "channels": []}""")

            assertEquals(SilencedChannelsLoad.Failed("Unsupported silenced channels version 2"), store().load())
        }

    @Test
    fun `an unreadable file fails to load`() =
        runTest {
            file().writeText(SilencedChannels.NONE.toJson())
            file().setReadable(false)

            assertInstanceOf(SilencedChannelsLoad.Failed::class.java, store().load())
        }

    @Test
    fun `an unwritable location fails to save`() =
        runTest {
            File(directory, "blocker").writeText("a file where a directory should be")
            val store = SilencedChannelsStore(File(directory, "blocker/silenced-channels.json"))

            assertInstanceOf(SilencedChannelsSave.Failed::class.java, store.save(SilencedChannels.NONE))
        }

    private fun silenced(): SilencedChannels {
        val rules =
            assertInstanceOf(
                RuleSetParse.Valid::class.java,
                Settings.DEFAULT.rules.withRules(
                    Settings.DEFAULT.rules.rules.map {
                        it.copy(
                            policy = Policy.HoldDuring,
                            apps = ruleApps(setOf(Target.App(slack)), emptySet()),
                        )
                    },
                ),
            ).rules
        return rules
            .planChannels(
                mapOf(messages to ReadChannel(Importance.High, ChannelKind.Shared, ChannelGrouping.Ungrouped)),
                SilencedChannels.NONE,
                ZonedDateTime.parse("2026-10-14T10:00+01:00[Europe/London]"),
            ).silenced
    }

    private fun store(): SilencedChannelsStore = SilencedChannelsStore(file())

    private fun file(): File = File(directory, "silenced-channels.json")
}
