package dev.danielgilchrist.knockoff.channel

import android.app.NotificationChannel
import android.os.Build
import android.os.Process
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.annotation.RequiresApi
import dev.danielgilchrist.knockoff.LOG_TAG
import dev.danielgilchrist.knockoff.domain.channel.ChannelChange
import dev.danielgilchrist.knockoff.domain.channel.ChannelGrouping
import dev.danielgilchrist.knockoff.domain.channel.ChannelKey
import dev.danielgilchrist.knockoff.domain.channel.ChannelKind
import dev.danielgilchrist.knockoff.domain.channel.ReadChannel
import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.ConversationId
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.Target

internal class ChannelAccess(
    private val listener: NotificationListenerService,
) {
    fun read(packages: Set<PackageName>): ChannelRead =
        packages.fold<PackageName, ChannelRead>(ChannelRead.Read(emptyMap())) { read, packageName ->
            when (read) {
                is ChannelRead.Read -> {
                    when (val fetch = fetch(packageName)) {
                        ChannelFetch.Unbound -> {
                            ChannelRead.Unbound
                        }

                        is ChannelFetch.NotLinked -> {
                            ChannelRead.NotLinked(fetch.detail)
                        }

                        is ChannelFetch.Fetched -> {
                            ChannelRead.Read(read.channels + readingsOf(packageName, fetch.channels))
                        }
                    }
                }

                is ChannelRead.NotLinked, ChannelRead.Unbound -> {
                    read
                }
            }
        }

    fun create(chat: Target.Chat): ChannelCreation =
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA -> ChannelCreation.Unsupported
            else -> createConversation(chat)
        }

    @RequiresApi(Build.VERSION_CODES.BAKLAVA)
    private fun createConversation(chat: Target.Chat): ChannelCreation =
        try {
            when (
                listener.createConversationNotificationChannelForPackage(
                    chat.packageName.value,
                    Process.myUserHandle(),
                    chat.channel.value,
                    chat.id.value,
                )
            ) {
                null -> ChannelCreation.Refused
                else -> ChannelCreation.Created
            }
        } catch (exception: SecurityException) {
            ChannelCreation.NotLinked(exception.toString())
        }

    fun apply(change: ChannelChange): ChannelUpdate = channel(change.key) { update(change, it) }

    private fun update(
        change: ChannelChange,
        channel: NotificationChannel,
    ): ChannelUpdate {
        channel.importance = AndroidImportance.level(change.to)

        return try {
            listener.updateNotificationChannel(change.key.packageName.value, Process.myUserHandle(), channel)
            confirm(change)
        } catch (exception: SecurityException) {
            ChannelUpdate.NotLinked(exception.toString())
        } catch (_: IllegalArgumentException) {
            ChannelUpdate.Missing
        }
    }

    private fun confirm(change: ChannelChange): ChannelUpdate =
        channel(change.key) { channel ->
            when (channel.importance) {
                AndroidImportance.level(change.to) -> ChannelUpdate.Applied
                else -> ChannelUpdate.Unconfirmed("importance is ${channel.importance} after the update")
            }
        }

    private inline fun channel(
        key: ChannelKey,
        found: (NotificationChannel) -> ChannelUpdate,
    ): ChannelUpdate =
        when (val fetch = fetch(key.packageName)) {
            ChannelFetch.Unbound -> {
                ChannelUpdate.Unbound
            }

            is ChannelFetch.NotLinked -> {
                ChannelUpdate.NotLinked(fetch.detail)
            }

            is ChannelFetch.Fetched -> {
                when (val channel = fetch.channels.firstOrNull { it.id == key.channelId.value }) {
                    null -> ChannelUpdate.Missing
                    else -> found(channel)
                }
            }
        }

    private fun readingsOf(
        packageName: PackageName,
        channels: List<NotificationChannel>,
    ): Map<ChannelKey, ReadChannel> =
        channels
            .flatMap { channel ->
                val key = ChannelKey(packageName, ChannelId(channel.id))

                when (val parse = AndroidImportance.parse(channel.importance)) {
                    is ImportanceParse.Known -> {
                        listOf(key to ReadChannel(parse.importance, kindOf(channel), ChannelGrouping.of(channel.group)))
                    }

                    is ImportanceParse.Unknown -> {
                        Log.w(LOG_TAG, "${key.describe()} skipped, unknown importance ${parse.level}")
                        emptyList()
                    }
                }
            }.toMap()

    private fun kindOf(channel: NotificationChannel): ChannelKind {
        val parent = channel.parentChannelId
        val conversation = channel.conversationId

        return when {
            parent == null || conversation == null -> ChannelKind.Shared
            else -> ChannelKind.ConversationOf(ChannelId(parent), ConversationId(conversation))
        }
    }

    private fun fetch(packageName: PackageName): ChannelFetch =
        try {
            when (val channels = listener.getNotificationChannels(packageName.value, Process.myUserHandle())) {
                null -> ChannelFetch.Unbound
                else -> ChannelFetch.Fetched(channels)
            }
        } catch (exception: SecurityException) {
            ChannelFetch.NotLinked(exception.toString())
        }
}
