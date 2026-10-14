package dev.danielgilchrist.knockoff.settings

import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleName
import dev.danielgilchrist.knockoff.domain.rule.RuleNameParse
import dev.danielgilchrist.knockoff.domain.rule.RuleSet
import dev.danielgilchrist.knockoff.domain.rule.RuleSetParse
import dev.danielgilchrist.knockoff.domain.settings.Settings
import dev.danielgilchrist.knockoff.domain.settings.SettingsDecode
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.plus
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
            val firstOwner = Job()

            assertEquals(
                SettingsSave.Saved,
                SettingsStore.create(settingsFile(), this + firstOwner).save(Settings.DEFAULT),
            )
            firstOwner.cancelAndJoin()

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
    fun `loads end after a read failure so callers must read again`() =
        runTest {
            settingsFile().writeText(Settings.DEFAULT.toJson())
            settingsFile().setReadable(false)

            val store = store()
            val loads = store.loads.toList()

            assertInstanceOf(SettingsLoad.ReadFailed::class.java, loads.single())

            settingsFile().setReadable(true)

            assertEquals(SettingsLoad.Loaded(Settings.DEFAULT), store.loads.first())
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
            val store = SettingsStore.create(File(directory, "blocker/settings.json"), backgroundScope)

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

    private fun TestScope.store(): SettingsStore = SettingsStore.create(settingsFile(), backgroundScope)
}
