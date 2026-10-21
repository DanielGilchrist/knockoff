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
    ) {
        val group = "^ncg_1_account_$account"
        val channel = "^nc_1_mail_$account"

        manager.createNotificationChannelGroup(NotificationChannelGroup(group, account))
        manager.createNotificationChannel(
            NotificationChannel(channel, "Mail", NotificationManager.IMPORTANCE_HIGH).apply { this.group = group },
        )
        manager.notify(
            account,
            (from + subject).hashCode(),
            Notification
                .Builder(context, channel)
                .setSmallIcon(android.R.drawable.sym_action_email)
                .setCategory(Notification.CATEGORY_EMAIL)
                .setContentTitle(from)
                .setContentText(subject)
                .setSubText(account)
                .setStyle(Notification.BigTextStyle().bigText(subject))
                .build(),
        )
    }
}
