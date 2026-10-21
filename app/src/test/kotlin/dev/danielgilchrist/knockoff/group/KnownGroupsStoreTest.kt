package dev.danielgilchrist.knockoff.group

import dev.danielgilchrist.knockoff.domain.channel.ChannelGrouping
import dev.danielgilchrist.knockoff.domain.channel.ChannelKey
import dev.danielgilchrist.knockoff.domain.group.KnownGroups
import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class KnownGroupsStoreTest {
    @TempDir
    lateinit var directory: File

    private val channel = ChannelKey(PackageName("com.google.android.gm"), ChannelId("^nc_1_mail_dan@work.example"))
    private val grouped = ChannelGrouping.Grouped(ChannelGroupId("^ncg_1_account_dan@work.example"))
    private val sighted = KnownGroups.NONE.sighted(channel, grouped)

    @Test
    fun `a missing file knows no groups`() =
        runTest {
            assertEquals(KnownGroupsLoad.Loaded(KnownGroups.NONE), store().loads.first())
            assertEquals(KnownGroups.NONE, store().current())
        }

    @Test
    fun `an update is recorded and loads back`() =
        runTest {
            val store = store()

            assertEquals(GroupRecord.Recorded, store.update { it.sighted(channel, grouped) })
            assertEquals(KnownGroupsLoad.Loaded(sighted), store.loads.first())
            assertEquals(sighted, store.current())
        }

    @Test
    fun `an update that changes nothing isn't written`() =
        runTest {
            val store = store()
            store.update { it.sighted(channel, grouped) }

            assertEquals(GroupRecord.Unchanged, store.update { it.sighted(channel, grouped) })
        }

    @Test
    fun `a corrupt file counts as knowing nothing and is replaced`() =
        runTest {
            file().writeText("nope")
            val store = store()

            assertInstanceOf(KnownGroupsLoad.Corrupt::class.java, store.loads.first())
            assertEquals(KnownGroups.NONE, store.current())
            assertEquals(GroupRecord.Recorded, store.update { it.sighted(channel, grouped) })
            assertEquals(sighted, store.current())
        }

    @Test
    fun `an unreadable file is left alone`() =
        runTest {
            val store = store()
            store.update { it.sighted(channel, grouped) }
            file().setReadable(false)

            assertInstanceOf(KnownGroupsLoad.ReadFailed::class.java, store.loads.first())
            assertEquals(KnownGroups.NONE, store.current())
            assertInstanceOf(GroupRecord.Failed::class.java, store.update { KnownGroups.NONE })
            file().setReadable(true)
            assertEquals(sighted, store.current())
        }

    @Test
    fun `groups that can't be written fail`() =
        runTest {
            directory.setWritable(false)

            val record = store().update { it.sighted(channel, grouped) }

            directory.setWritable(true)
            assertInstanceOf(GroupRecord.Failed::class.java, record)
        }

    private fun store(): KnownGroupsStore = KnownGroupsStore(file())

    private fun file(): File = File(directory, "channel-groups.json")
}
