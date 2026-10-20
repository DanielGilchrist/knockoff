package dev.danielgilchrist.knockoff.fakechat

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Person
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager

class FakeNotifications(
    private val context: Context,
) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    init {
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Messages", NotificationManager.IMPORTANCE_HIGH))
    }

    fun chat(
        chat: String,
        messages: List<String>,
    ) {
        val person =
            Person
                .Builder()
                .setName(chat)
                .setKey(idOf(chat))
                .build()
        val style = Notification.MessagingStyle(Person.Builder().setName("Me").build())
        val now = System.currentTimeMillis()

        publishShortcut(chat, person)
        messages.forEachIndexed { index, text -> style.addMessage(text, now - (messages.size - index), person) }
        manager.notify(
            chat,
            ID,
            builder()
                .setStyle(style)
                .setShortcutId(idOf(chat))
                .setOnlyAlertOnce(true)
                .build(),
        )
    }

    fun summary(chats: Map<String, List<String>>) {
        val count = chats.values.sumOf { it.size }
        val inbox = Notification.InboxStyle().setSummaryText("$count messages from ${chats.size} chats")

        chats.forEach { (chat, messages) -> messages.forEach { inbox.addLine("$chat: $it") } }
        manager.notify(
            null,
            ID,
            builder()
                .setContentTitle("FakeChat")
                .setContentText("$count messages from ${chats.size} chats")
                .setStyle(inbox)
                .setShortcutId(idOf(chats.keys.first()))
                .setGroupSummary(true)
                .setOnlyAlertOnce(true)
                .build(),
        )
    }

    fun cancel(chat: String) {
        manager.cancel(chat, ID)
    }

    fun cancelSummary() {
        manager.cancel(null, ID)
    }

    fun cancelAll() {
        manager.cancelAll()
    }

    private fun builder(): Notification.Builder =
        Notification
            .Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.sym_action_chat)
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setGroup(GROUP)

    private fun publishShortcut(
        chat: String,
        person: Person,
    ) {
        context.getSystemService(ShortcutManager::class.java).pushDynamicShortcut(
            ShortcutInfo
                .Builder(context, idOf(chat))
                .setShortLabel(chat)
                .setLongLived(true)
                .setPerson(person)
                .setCategories(setOf(ShortcutInfo.SHORTCUT_CATEGORY_CONVERSATION))
                .setIntent(Intent(Intent.ACTION_VIEW).setClass(context, OpenActivity::class.java))
                .build(),
        )
    }

    private fun idOf(chat: String): String = "${chat.lowercase().replace(' ', '.')}@lid"

    private companion object {
        const val CHANNEL = "individual_chat_defaults"
        const val GROUP = "group_key_messages"
        const val ID = 1
    }
}
