package dev.danielgilchrist.knockoff.channel

import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.group.GroupDiscovery
import dev.danielgilchrist.knockoff.group.GroupReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class GroupDiscoverer(
    private val access: ChannelAccess,
    private val learning: GroupLearning,
) : GroupReader {
    override suspend fun discover(packageName: PackageName): GroupDiscovery =
        withContext(Dispatchers.IO) {
            when (val read = access.read(setOf(packageName))) {
                is ChannelRead.Read -> learning.learn(packageName, read.channels)
                is ChannelRead.NotLinked -> GroupDiscovery.NotLinked(read.detail)
                ChannelRead.Unbound -> GroupDiscovery.Unbound
            }
        }
}
