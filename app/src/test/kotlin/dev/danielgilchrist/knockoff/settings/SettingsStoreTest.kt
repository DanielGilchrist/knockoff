package dev.danielgilchrist.knockoff.settings

import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleName
import dev.danielgilchrist.knockoff.domain.rule.RuleNameParse
import dev.danielgilchrist.knockoff.domain.rule.RuleSet
import dev.danielgilchrist.knockoff.domain.rule.RuleSetParse
import dev.danielgilchrist.knockoff.domain.settings.Settings
import dev.danielgilchrist.knockoff.domain.settings.SettingsDecode
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class SettingsStoreTest {
    @TempDir
    lateinit var directory: File

    @Test
    fun `a missing file loads as empty`() =
        runTest {
            assertEquals(SettingsLoad.Empty, store().loads.first())
        }

    @Test
    fun `saved settings survive a new store on the same file`() =
        runTest {
            assertEquals(SettingsSave.Saved, store().save(Settings.DEFAULT))

            assertEquals(SettingsLoad.Loaded(Settings.DEFAULT), store().loads.first())
        }

    @Test
    fun `a corrupt file loads as corrupt with its decode failure`() =
        runTest {
            settingsFile().writeText("""{"version": 9, "rules": []}""")

            assertEquals(SettingsLoad.Corrupt(SettingsDecode.UnsupportedVersion(9)), store().loads.first())
        }

    @Test
    fun `saving replaces a corrupt file`() =
        runTest {
            settingsFile().writeText("not json")
            val store = store()

            assertEquals(SettingsSave.Saved, store.save(Settings.DEFAULT))
            assertEquals(SettingsLoad.Loaded(Settings.DEFAULT), store.loads.first())
        }

    @Test
    fun `a read failure is read again by the next collector`() =
        runTest {
            settingsFile().writeText(Settings.DEFAULT.toJson())
            settingsFile().setReadable(false)
            val store = store()

            assertInstanceOf(SettingsLoad.ReadFailed::class.java, store.loads.first())

            settingsFile().setReadable(true)

            assertEquals(SettingsLoad.Loaded(Settings.DEFAULT), store.loads.first())
        }

    @Test
    fun `a collector sees each saved change`() =
        runTest {
            val store = store()
            val evenings = Settings(ruleSet(work(), evenings()))
            val loads = collect(store)

            assertEquals(SettingsLoad.Empty, loads.receive())
            store.save(Settings.DEFAULT)
            assertEquals(SettingsLoad.Loaded(Settings.DEFAULT), loads.receive())
            store.save(evenings)
            assertEquals(SettingsLoad.Loaded(evenings), loads.receive())
        }

    @Test
    fun `a save after a read failure is seen by the same collector`() =
        runTest {
            settingsFile().writeText(Settings.DEFAULT.toJson())
            settingsFile().setReadable(false)
            val store = store()
            val evenings = Settings(ruleSet(work(), evenings()))
            val loads = collect(store)

            assertInstanceOf(SettingsLoad.ReadFailed::class.java, loads.receive())
            store.save(evenings)
            assertEquals(SettingsLoad.Loaded(evenings), loads.receive())
        }

    @Test
    fun `an update that can't be written is an input-output failure and changes nothing`() =
        runTest {
            settingsFile().writeText(Settings.DEFAULT.toJson())
            directory.setWritable(false)

            val update = store().update { RuleSet.parse(it.rules.rules + evenings()) }

            directory.setWritable(true)
            assertInstanceOf(SettingsUpdate.IoFailed::class.java, update)
            assertEquals(Settings.DEFAULT.toJson(), settingsFile().readText())
        }

    @Test
    fun `saving leaves only the settings file behind`() =
        runTest {
            store().save(Settings.DEFAULT)

            assertEquals(listOf("settings.json"), directory.list()?.toList())
        }

    @Test
    fun `invalid text encoding in an otherwise valid document loads as corrupt`() =
        runTest {
            val (before, after) = Settings.DEFAULT.toJson().split("Work")
            settingsFile().writeBytes(
                before.encodeToByteArray() + byteArrayOf(0xC3.toByte()) + after.encodeToByteArray(),
            )

            val load = assertInstanceOf(SettingsLoad.Corrupt::class.java, store().loads.first())

            assertInstanceOf(SettingsDecode.Unreadable::class.java, load.failure)
        }

    @Test
    fun `an unwritable location saves as a write failure`() =
        runTest {
            File(directory, "blocker").writeText("a file where a directory should be")
            val store = SettingsStore(File(directory, "blocker/settings.json"))

            assertInstanceOf(SettingsSave.WriteFailed::class.java, store.save(Settings.DEFAULT))
        }

    @Test
    fun `an update to a missing file starts from the defaults`() =
        runTest {
            val store = store()

            assertEquals(SettingsUpdate.Saved, store.update { RuleSet.parse(it.rules.rules + evenings()) })
            assertEquals(SettingsLoad.Loaded(Settings(ruleSet(work(), evenings()))), store.loads.first())
        }

    @Test
    fun `updates apply to the latest saved settings`() =
        runTest {
            val store = store()

            assertEquals(SettingsUpdate.Saved, store.update { RuleSet.parse(it.rules.rules + evenings()) })
            assertEquals(
                SettingsUpdate.Saved,
                store.update {
                    RuleSet.parse(it.rules.rules - work())
                },
            )

            assertEquals(SettingsLoad.Loaded(Settings(ruleSet(evenings()))), store.loads.first())
        }

    @Test
    fun `concurrent updates are all kept`() =
        runTest {
            val store = store()
            val names = (1..10).map { "Rule $it" }

            names
                .map { name ->
                    async {
                        store.update { RuleSet.parse(it.rules.rules + work().copy(name = ruleName(name))) }
                    }
                }.awaitAll()

            val loaded = assertInstanceOf(SettingsLoad.Loaded::class.java, store.loads.first())
            assertEquals(
                setOf("Work") + names,
                loaded.settings.rules.rules
                    .map { it.name.value }
                    .toSet(),
            )
        }

    @Test
    fun `an update producing duplicate names changes nothing`() =
        runTest {
            val store = store()

            val update = store.update { RuleSet.parse(it.rules.rules + work()) }

            assertEquals(SettingsUpdate.DuplicateNames(setOf(work().name)), update)
            assertEquals(SettingsLoad.Empty, store.loads.first())
        }

    @Test
    fun `an update to a corrupt file is refused and leaves it alone`() =
        runTest {
            settingsFile().writeText("not json")

            val update =
                assertInstanceOf(SettingsUpdate.NotLoaded::class.java, store().update { RuleSet.parse(it.rules.rules) })

            assertEquals("not json", settingsFile().readText())
            assertEquals(true, update.detail.isNotEmpty())
        }

    @Test
    fun `an update to an unreadable file is an input-output failure`() =
        runTest {
            settingsFile().writeText(Settings.DEFAULT.toJson())
            settingsFile().setReadable(false)

            assertInstanceOf(SettingsUpdate.IoFailed::class.java, store().update { RuleSet.parse(it.rules.rules) })
        }

    private fun work(): Rule =
        Settings.DEFAULT.rules.rules
            .single()

    private fun evenings(): Rule = work().copy(name = ruleName("Evenings"))

    private fun ruleName(raw: String): RuleName =
        assertInstanceOf(RuleNameParse.Valid::class.java, RuleName.parse(raw)).name

    private fun ruleSet(vararg rules: Rule): RuleSet =
        assertInstanceOf(RuleSetParse.Valid::class.java, RuleSet.parse(rules.toList())).rules

    private fun settingsFile(): File = File(directory, "settings.json")

    private fun store(): SettingsStore = SettingsStore(settingsFile())

    private fun TestScope.collect(store: SettingsStore): ReceiveChannel<SettingsLoad> {
        val loads = Channel<SettingsLoad>(Channel.UNLIMITED)
        backgroundScope.launch { store.loads.collect(loads::send) }
        return loads
    }
}
