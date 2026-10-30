package dev.danielgilchrist.knockoff.channel

import android.app.NotificationChannelGroup
import android.os.Process
import android.service.notification.NotificationListenerService
import dev.danielgilchrist.knockoff.domain.group.GroupName
import dev.danielgilchrist.knockoff.domain.group.GroupNameParse
import dev.danielgilchrist.knockoff.domain.notification.ChannelGroupId
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.system.SystemCall
import dev.danielgilchrist.knockoff.system.systemCall

internal class GroupNames(
    private val listener: NotificationListenerService,
) : GroupNameSource {
    override fun of(packageName: PackageName): GroupNamesFetch =
        when (
            val call = systemCall { listener.getNotificationChannelGroups(packageName.value, Process.myUserHandle()) }
        ) {
            is SystemCall.Done -> call.value?.let { GroupNamesFetch.Fetched(namesOf(it)) } ?: GroupNamesFetch.Unbound
            is SystemCall.Refused -> GroupNamesFetch.NotLinked(call.detail)
            is SystemCall.Unavailable -> GroupNamesFetch.Unavailable(call.detail)
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
