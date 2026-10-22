package dev.danielgilchrist.knockoff.listener

import android.app.Notification
import dev.danielgilchrist.knockoff.domain.notification.GroupRole
import dev.danielgilchrist.knockoff.domain.notification.NotificationKey
import dev.danielgilchrist.knockoff.domain.notification.NotificationSnapshot
import dev.danielgilchrist.knockoff.domain.notification.NotificationStyle
import dev.danielgilchrist.knockoff.domain.notification.OriginGroup
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GroupEntryTest {
    private val summary = entry("summary", "chats", GroupPosition.Summary)
    private val boss = entry("boss", "chats")
    private val mum = entry("mum", "chats")
    private val elsewhere = entry("other", "updates")
    private val otherSummary = entry("other-summary", "chats", GroupPosition.Summary)
    private val otherApp = entry("slack", "chats", packageName = "com.Slack")

    @Test
    fun `a summary sees the other notifications showing in its group`() {
        val active = ActiveGroups.Read(listOf(summary, boss, mum, elsewhere, otherSummary, otherApp))

        assertEquals(
            GroupRoleLookup.Found(GroupRole.Summary(listOf(boss.posted, mum.posted))),
            summary.role { active },
        )
    }

    @Test
    fun `a summary with nothing showing under it sees nothing`() {
        assertEquals(
            GroupRoleLookup.Found(GroupRole.Summary(emptyList())),
            summary.role { ActiveGroups.Read(listOf(summary)) },
        )
    }

    @Test
    fun `a summary's group is unknown when active notifications can't be read`() {
        assertEquals(GroupRoleLookup.Unknown("refused"), summary.role { ActiveGroups.Unavailable("refused") })
    }

    @Test
    fun `anything else is a member without reading the group`() {
        assertEquals(GroupRoleLookup.Found(GroupRole.Member), boss.role { error("the group was read") })
    }

    @Test
    fun `only a grouped notification flagged as its summary is a summary`() {
        assertEquals(GroupPosition.Summary, GroupEntry.positionOf("chats", Notification.FLAG_GROUP_SUMMARY))
        assertEquals(GroupPosition.Member, GroupEntry.positionOf(null, Notification.FLAG_GROUP_SUMMARY))
        assertEquals(GroupPosition.Member, GroupEntry.positionOf("chats", Notification.FLAG_AUTO_CANCEL))
    }

    @Test
    fun `a summary Android made for ungrouped notifications is an autogroup summary`() {
        val autogroup = Notification.FLAG_GROUP_SUMMARY or FLAG_AUTOGROUP_SUMMARY

        assertEquals(GroupPosition.AutogroupSummary, GroupEntry.positionOf("g:Aggregate_AlertingSection", autogroup))
        assertEquals(GroupPosition.Member, GroupEntry.positionOf(null, autogroup))
        assertEquals(GroupPosition.Member, GroupEntry.positionOf("g:Aggregate_AlertingSection", FLAG_AUTOGROUP_SUMMARY))
    }

    @Test
    fun `an autogroup summary sees what shows under it`() {
        val autogroup = entry("autogroup", "chats", GroupPosition.AutogroupSummary)

        assertEquals(
            GroupRoleLookup.Found(GroupRole.Summary(listOf(boss.posted))),
            autogroup.role { ActiveGroups.Read(listOf(autogroup, boss, elsewhere)) },
        )
    }

    @Test
    fun `a summary can be dismissed only when nothing in its group is active`() {
        val autogroup = entry("autogroup", "chats", GroupPosition.AutogroupSummary)

        assertEquals(DismissCheck.Alone, autogroup.dismissCheck(listOf(autogroup, elsewhere, otherSummary, otherApp)))
        assertEquals(DismissCheck.Occupied(2), autogroup.dismissCheck(listOf(autogroup, boss, mum, elsewhere)))
    }

    @Test
    fun `a group is shared only within one app`() {
        assertEquals(true, summary.sharesGroupWith(boss))
        assertEquals(false, summary.sharesGroupWith(summary))
        assertEquals(false, summary.sharesGroupWith(elsewhere))
        assertEquals(false, summary.sharesGroupWith(otherApp))
    }

    private fun entry(
        key: String,
        group: String,
        position: GroupPosition = GroupPosition.Member,
        packageName: String = "com.whatsapp",
    ): GroupEntry =
        GroupEntry(
            key,
            packageName,
            group,
            position,
            NotificationSnapshot(
                NotificationKey(key),
                PackageName("com.whatsapp"),
                channelId = null,
                group = OriginGroup.Unknown,
                style = NotificationStyle.Other,
                shortcutId = null,
                titles = emptyList(),
                traits = emptySet(),
            ).parse(),
        )
}
