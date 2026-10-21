package dev.danielgilchrist.knockoff.domain.rule

import dev.danielgilchrist.knockoff.domain.notification.PackageName

sealed interface PatternPick {
    val pattern: TextPattern

    fun inApp(packageName: PackageName): Target.Pattern

    data class Channel(
        override val pattern: TextPattern,
    ) : PatternPick {
        override fun inApp(packageName: PackageName): Target.Pattern = Target.ChannelPattern(packageName, pattern)
    }

    data class ChatName(
        override val pattern: TextPattern,
    ) : PatternPick {
        override fun inApp(packageName: PackageName): Target.Pattern = Target.ChatNamePattern(packageName, pattern)
    }

    companion object {
        fun of(target: Target.Pattern): PatternPick =
            when (target) {
                is Target.ChannelPattern -> Channel(target.pattern)
                is Target.ChatNamePattern -> ChatName(target.pattern)
            }
    }
}
