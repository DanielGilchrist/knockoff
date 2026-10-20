package dev.danielgilchrist.knockoff.listener

import android.app.Notification
import android.service.notification.StatusBarNotification
import dev.danielgilchrist.knockoff.domain.notification.GroupRole
import dev.danielgilchrist.knockoff.domain.notification.PostedNotification

internal data class GroupEntry(
    val key: String,
    val packageName: String,
    val group: String,
    val position: GroupPosition,
    val posted: PostedNotification,
) {
    fun role(groups: () -> ActiveGroups): GroupRoleLookup =
        when (position) {
            GroupPosition.Member -> {
                GroupRoleLookup.Found(GroupRole.Member)
            }

            GroupPosition.Summary -> {
                when (val read = groups()) {
                    is ActiveGroups.Read -> GroupRoleLookup.Found(GroupRole.Summary(showingUnder(read.entries)))
                    is ActiveGroups.Unavailable -> GroupRoleLookup.Unknown(read.detail)
                }
            }
        }

    fun sharesGroupWith(other: GroupEntry): Boolean =
        other.packageName == packageName && other.group == group && other.key != key

    private fun showingUnder(active: List<GroupEntry>): List<PostedNotification> =
        active.filter { sharesGroupWith(it) && it.position == GroupPosition.Member }.map { it.posted }

    companion object {
        fun of(sbn: StatusBarNotification): GroupEntry = of(sbn, sbn.toSnapshot().parse())

        fun of(
            sbn: StatusBarNotification,
            posted: PostedNotification,
        ): GroupEntry =
            GroupEntry(
                key = sbn.key,
                packageName = sbn.packageName,
                group = sbn.groupKey,
                position = positionOf(sbn.notification.group, sbn.notification.flags),
                posted = posted,
            )

        fun positionOf(
            group: String?,
            flags: Int,
        ): GroupPosition =
            when {
                group != null && flags and Notification.FLAG_GROUP_SUMMARY != 0 -> GroupPosition.Summary
                else -> GroupPosition.Member
            }
    }
}
