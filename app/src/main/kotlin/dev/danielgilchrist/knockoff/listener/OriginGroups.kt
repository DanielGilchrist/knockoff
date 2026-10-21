package dev.danielgilchrist.knockoff.listener

import android.service.notification.NotificationListenerService
import android.service.notification.NotificationListenerService.Ranking
import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification
import dev.danielgilchrist.knockoff.domain.channel.ChannelGrouping
import dev.danielgilchrist.knockoff.domain.notification.OriginGroup

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

internal fun NotificationListenerService.groupsNow(): (StatusBarNotification) -> OriginGroup =
    when (val rankings = currentRanking) {
        null -> { _ -> OriginGroup.Unknown }
        else -> rankings::groupOf
    }
