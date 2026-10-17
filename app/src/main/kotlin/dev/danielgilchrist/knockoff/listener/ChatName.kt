package dev.danielgilchrist.knockoff.listener

import android.app.Notification
import android.service.notification.NotificationListenerService.Ranking
import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification

internal fun StatusBarNotification.chatTitles(): List<String> =
    listOfNotNull(
        notification.extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE),
        notification.extras.getCharSequence(Notification.EXTRA_TITLE),
    ).map(CharSequence::toString)

internal fun StatusBarNotification.shortcutLabels(rankings: RankingMap): List<String> {
    val ranking = Ranking()

    return when {
        rankings.getRanking(key, ranking) -> listOfNotNull(ranking.conversationShortcutInfo?.shortLabel?.toString())
        else -> emptyList()
    }
}
