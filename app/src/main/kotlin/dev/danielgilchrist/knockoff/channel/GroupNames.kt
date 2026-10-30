package dev.danielgilchrist.knockoff.channel

import android.app.NotificationChannelGroup
import android.os.BadParcelableException
import android.os.Process
import android.service.notification.NotificationListenerService
import dev.danielgilchrist.knockoff.domain.group.GroupName
import dev.danielgilchrist.knockoff.domain.group.GroupNameParse
import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId
import dev.danielgilchrist.knockoff.domain.notification.PackageName

internal class GroupNames(
    private val listener: NotificationListenerService,
) : GroupNameSource {
    override fun of(packageName: PackageName): GroupNamesFetch =
        try {
            when (val groups = listener.getNotificationChannelGroups(packageName.value, Process.myUserHandle())) {
                null -> GroupNamesFetch.Unbound
                else -> GroupNamesFetch.Fetched(namesOf(groups))
            }
        } catch (exception: SecurityException) {
            GroupNamesFetch.NotLinked(exception.toString())
        } catch (exception: BadParcelableException) {
            GroupNamesFetch.Unavailable(exception.toString())
        }

    private fun namesOf(groups: List<NotificationChannelGroup>): Map<ChannelGroupId, GroupName> =
        groups
            .flatMap { group ->
                when (val name = GroupName.parse(group.name?.toString().orEmpty())) {
                    is GroupNameParse.Valid -> listOf(ChannelGroupId(group.id) to name.name)
                    GroupNameParse.Blank -> emptyList()
                }
            }.toMap()
}
