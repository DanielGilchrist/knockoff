package dev.danielgilchrist.knockoff

import dev.danielgilchrist.knockoff.domain.notification.ChannelId
import dev.danielgilchrist.knockoff.domain.notification.ConversationId
import dev.danielgilchrist.knockoff.domain.notification.ConversationName
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.Target

val COLLEAGUE_CHAT =
    Target.Chat(
        PackageName("com.whatsapp"),
        ChannelId("individual_chat_defaults_4"),
        ConversationId("1000000000001@lid"),
        ConversationName("Colleague"),
    )
