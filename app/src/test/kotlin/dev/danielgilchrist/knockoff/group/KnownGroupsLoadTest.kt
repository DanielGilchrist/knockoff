package dev.danielgilchrist.knockoff.group

import dev.danielgilchrist.knockoff.domain.channel.ChannelGrouping
import dev.danielgilchrist.knockoff.domain.channel.ChannelKey
import dev.danielgilchrist.knockoff.domain.group.KnownGroups
import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class KnownGroupsLoadTest {
    @Test
    fun `only loaded groups are known, anything unreadable knows none`() {
        val groups =
            KnownGroups.NONE.sighted(
                ChannelKey(PackageName("com.google.android.gm"), ChannelId("mail")),
                ChannelGrouping.Grouped(ChannelGroupId("inbox")),
            )

        assertEquals(groups, KnownGroupsLoad.Loaded(groups).known)
        assertEquals(KnownGroups.NONE, KnownGroupsLoad.Corrupt("x").known)
        assertEquals(KnownGroups.NONE, KnownGroupsLoad.ReadFailed("x").known)
    }
}
