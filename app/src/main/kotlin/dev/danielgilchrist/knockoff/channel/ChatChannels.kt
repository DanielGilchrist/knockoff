package dev.danielgilchrist.knockoff.channel

import android.util.Log
import dev.danielgilchrist.knockoff.LOG_TAG
import dev.danielgilchrist.knockoff.domain.chat.ChatChannelAttempts
import dev.danielgilchrist.knockoff.domain.chat.ChatChannelNeed
import dev.danielgilchrist.knockoff.domain.rule.Target

internal class ChatChannels(
    private val access: ChannelAccess,
) {
    private var attempts = ChatChannelAttempts.NONE

    fun create(needs: Set<ChatChannelNeed>): ChatChannelsPass {
        val plan = attempts.plan(needs)
        attempts = plan.next
        plan.newlyMissing.forEach { Log.w(LOG_TAG, "${describe(it)} not created, its parent channel is gone") }
        val created = plan.create.map(::create)

        val creation =
            when {
                ChatChannelsCreation.SomeCreated in created -> ChatChannelsCreation.SomeCreated
                else -> ChatChannelsCreation.NoneCreated
            }

        return ChatChannelsPass(creation, plan.waiting)
    }

    private fun create(chat: Target.Chat): ChatChannelsCreation {
        when (val creation = access.create(chat)) {
            ChannelCreation.Created -> {
                Log.i(LOG_TAG, "${describe(chat)} created")
                return ChatChannelsCreation.SomeCreated
            }

            ChannelCreation.Refused -> {
                attempts = attempts.refused(chat)
                Log.w(LOG_TAG, "${describe(chat)} refused: unbound, at the channel limit, or its parent is a bundle")
            }

            ChannelCreation.Unsupported -> {
                attempts = attempts.unsupported(chat)
                Log.w(LOG_TAG, "${describe(chat)} unsupported before Android 16")
            }

            is ChannelCreation.NotLinked -> {
                Log.w(LOG_TAG, "${describe(chat)} not created, not linked: ${creation.detail}")
            }
        }

        return ChatChannelsCreation.NoneCreated
    }

    private fun describe(chat: Target.Chat): String =
        "${chat.packageName.value} chat channel under ${chat.channel.value}"
}
