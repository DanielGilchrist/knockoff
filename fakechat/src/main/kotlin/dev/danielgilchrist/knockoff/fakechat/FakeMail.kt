package dev.danielgilchrist.knockoff.fakechat

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context

class FakeMail(
    private val context: Context,
) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    fun post(
        account: String,
        from: String,
        subject: String,
        style: MailStyle,
    ) {
        val channel =
            when (style) {
                MailStyle.Grouped -> groupedChannel(account)
                MailStyle.SubText, MailStyle.SubTextUncategorised -> sharedChannel()
                MailStyle.Category -> categoryChannel()
            }
        val category =
            when (style) {
                MailStyle.Grouped, MailStyle.SubText, MailStyle.Category -> Notification.CATEGORY_EMAIL
                MailStyle.SubTextUncategorised -> Notification.CATEGORY_STATUS
            }

        manager.notify(
            account,
            (from + subject).hashCode(),
            Notification
                .Builder(context, channel)
                .setSmallIcon(android.R.drawable.sym_action_email)
                .setCategory(category)
                .setContentTitle(from)
                .setContentText(subject)
                .setSubText(account)
                .setStyle(Notification.BigTextStyle().bigText(subject))
                .build(),
        )
    }

    private fun groupedChannel(account: String): String {
        val group = "^ncg_1_account_$account"
        val channel = "^nc_1_mail_$account"

        manager.createNotificationChannelGroup(NotificationChannelGroup(group, account))
        manager.createNotificationChannel(
            NotificationChannel(channel, "Mail", NotificationManager.IMPORTANCE_HIGH).apply { this.group = group },
        )

        return channel
    }

    private fun sharedChannel(): String {
        manager.createNotificationChannel(NotificationChannel(SHARED, "Emails", NotificationManager.IMPORTANCE_HIGH))

        return SHARED
    }

    private fun categoryChannel(): String {
        manager.createNotificationChannelGroup(NotificationChannelGroup(UPDATES, "Updates"))
        manager.createNotificationChannel(
            NotificationChannel(UPDATES, "Updates", NotificationManager.IMPORTANCE_HIGH).apply { group = UPDATES },
        )

        return UPDATES
    }

    private companion object {
        const val SHARED = "emails"
        const val UPDATES = "updates"
    }
}
