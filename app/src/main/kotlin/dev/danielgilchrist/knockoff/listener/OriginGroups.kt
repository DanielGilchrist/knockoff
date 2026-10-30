package dev.danielgilchrist.knockoff.listener

import android.service.notification.NotificationListenerService.Ranking
import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification
import dev.danielgilchrist.knockoff.domain.channel.ChannelGrouping
import dev.danielgilchrist.knockoff.domain.channel.ChannelKey
import dev.danielgilchrist.knockoff.domain.group.KnownGroups
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.OriginGroup
import dev.danielgilchrist.knockoff.domain.notification.PackageName

internal fun RankingMap.groupOf(sbn: StatusBarNotification): OriginGroup {
    val ranking = Ranking()

    return when {
        getRanking(sbn.key, ranking) -> {
            when (val channel = ranking.channel) {
                null -> OriginGroup.Unknown
                else -> OriginGroup.Known(ChannelGrouping.of(channel.group))
            }
        }

        else -> {
            OriginGroup.Unknown
        }
    }
}

internal fun KnownGroups.groupOf(sbn: StatusBarNotification): OriginGroup =
    when (val channel = sbn.notification.channelId) {
        null -> OriginGroup.Unknown
        else -> groupOf(ChannelKey(PackageName(sbn.packageName), ChannelId(channel)))
    }
