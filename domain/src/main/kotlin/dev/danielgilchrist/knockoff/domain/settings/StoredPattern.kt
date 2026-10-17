package dev.danielgilchrist.knockoff.domain.settings

import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.Target
import dev.danielgilchrist.knockoff.domain.rule.TextPattern
import dev.danielgilchrist.knockoff.domain.rule.TextPatternParse
import kotlinx.serialization.Serializable

@Serializable
internal data class StoredPattern(
    private val app: String,
    private val field: String,
    private val pattern: String,
) {
    companion object {
        private const val CHANNEL = "channel"
        private const val CHAT_NAME = "chatName"

        fun from(target: Target.Pattern): StoredPattern =
            when (target) {
                is Target.ChannelPattern -> StoredPattern(target.packageName.value, CHANNEL, target.pattern.source)
                is Target.ChatNamePattern -> StoredPattern(target.packageName.value, CHAT_NAME, target.pattern.source)
            }
    }

    fun parse(): Parsed<Target.Pattern> =
        when (val parse = TextPattern.parse(pattern)) {
            TextPatternParse.Blank -> Parsed.Failed("blank pattern")
            is TextPatternParse.Invalid -> Parsed.Failed("invalid pattern \"$pattern\": ${parse.detail}")
            is TextPatternParse.Valid -> target(parse.pattern)
        }

    private fun target(textPattern: TextPattern): Parsed<Target.Pattern> =
        when (field) {
            CHANNEL -> Parsed.Ok(Target.ChannelPattern(PackageName(app), textPattern))
            CHAT_NAME -> Parsed.Ok(Target.ChatNamePattern(PackageName(app), textPattern))
            else -> Parsed.Failed("unknown pattern field \"$field\"")
        }
}
