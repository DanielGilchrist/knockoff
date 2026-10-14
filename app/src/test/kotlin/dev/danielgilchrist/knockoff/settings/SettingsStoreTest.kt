package dev.danielgilchrist.knockoff.settings

import dev.danielgilchrist.knockoff.domain.settings.Settings
import dev.danielgilchrist.knockoff.domain.settings.SettingsDecode
import kotlinx.coroutines.Job
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

    private fun settingsFile(): File = File(directory, "settings.json")

    private fun TestScope.store(): SettingsStore = SettingsStore.create(settingsFile(), backgroundScope)
}
