package dev.danielgilchrist.knockoff.listener

import android.app.Notification
import android.service.notification.NotificationListenerService.Ranking
import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification

internal fun StatusBarNotification.chatNames(rankings: RankingMap): List<String> {
    val ranking = Ranking()
    val label =
        when {
            rankings.getRanking(key, ranking) -> ranking.conversationShortcutInfo?.shortLabel
            else -> null
        }
    val extras = notification.extras

    return listOfNotNull(
        label,
        extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE),
        extras.getCharSequence(Notification.EXTRA_TITLE),
    ).map(CharSequence::toString)
}
