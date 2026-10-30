package dev.danielgilchrist.knockoff.channel

import dev.danielgilchrist.knockoff.domain.channel.ChannelGrouping
import dev.danielgilchrist.knockoff.domain.channel.ChannelKey
import dev.danielgilchrist.knockoff.domain.channel.ChannelKind
import dev.danielgilchrist.knockoff.domain.channel.Importance
import dev.danielgilchrist.knockoff.domain.channel.ReadChannel
import dev.danielgilchrist.knockoff.domain.group.GroupLabel
import dev.danielgilchrist.knockoff.domain.group.KnownGroup
import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.group.GroupDiscovery
import dev.danielgilchrist.knockoff.group.KnownGroupsStore
import dev.danielgilchrist.knockoff.groupName
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class GroupLearningTest {
    @TempDir
    lateinit var directory: File

    private val gmail = PackageName.GMAIL
    private val work = ChannelGroupId("^ncg_1_account_dan@work.example")
    private val mail = ChannelKey(gmail, ChannelId("^nc_1_mail_dan@work.example"))
    private val channels =
        mapOf(mail to ReadChannel(Importance.Default, ChannelKind.Shared, ChannelGrouping.Grouped(work)))
    private val named = GroupNamesFetch.Fetched(mapOf(work to groupName("dan@work.example")))

    @Test
    fun `read names and channels are stored and the app counts as read`() =
        runTest {
            val store = store()

            assertEquals(GroupDiscovery.Read, GroupLearning({ named }, store).learn(gmail, channels))
            assertEquals(
                listOf(KnownGroup(work, GroupLabel.Named(groupName("dan@work.example")))),
                store.current().groupsOf(gmail),
            )
            assertEquals(GroupDiscovery.Read, GroupLearning({ named }, store).learn(gmail, channels))
        }

    @Test
    fun `names that can't be read pass their reason through and store nothing`() =
        runTest {
            val store = store()

            assertEquals(
                GroupDiscovery.NotLinked("no association"),
                GroupLearning({ GroupNamesFetch.NotLinked("no association") }, store).learn(gmail, channels),
            )
            assertEquals(
                GroupDiscovery.Unbound,
                GroupLearning({ GroupNamesFetch.Unbound }, store).learn(gmail, channels),
            )
            assertEquals(
                GroupDiscovery.Unavailable("binder full"),
                GroupLearning({ GroupNamesFetch.Unavailable("binder full") }, store).learn(gmail, channels),
            )
            assertEquals(emptyList<KnownGroup>(), store.current().groupsOf(gmail))
        }

    @Test
    fun `a store that can't be written leaves the app unrecorded`() =
        runTest {
            val blocked = File(directory, "blocked").apply { mkdirs() }
            val store = KnownGroupsStore(File(blocked, "channel-groups.json"))
            blocked.setWritable(false)

            val learned = GroupLearning({ named }, store).learn(gmail, channels)

            blocked.setWritable(true)
            assertEquals(GroupDiscovery.Unrecorded::class, learned::class)
        }

    private fun store(): KnownGroupsStore = KnownGroupsStore(File(directory, "channel-groups.json"))
}
