package dev.danielgilchrist.knockoff.domain.settings

import dev.danielgilchrist.knockoff.domain.rule.PatternPick
import dev.danielgilchrist.knockoff.domain.rule.TextPattern
import dev.danielgilchrist.knockoff.domain.rule.TextPatternParse
import kotlinx.serialization.Serializable

@Serializable
internal data class StoredPatternPick(
    private val field: String,
    private val pattern: String,
) {
    companion object {
        const val CHANNEL = "channel"
        const val CHAT_NAME = "chatName"

        fun from(pick: PatternPick): StoredPatternPick =
            when (pick) {
                is PatternPick.Channel -> StoredPatternPick(CHANNEL, pick.pattern.source)
                is PatternPick.ChatName -> StoredPatternPick(CHAT_NAME, pick.pattern.source)
            }
    }

    fun parse(): Parsed<PatternPick> =
        when (val parse = TextPattern.parse(pattern)) {
            TextPatternParse.Blank -> Parsed.Failed("blank pattern")
            is TextPatternParse.Invalid -> Parsed.Failed("invalid pattern \"$pattern\": ${parse.detail}")
            is TextPatternParse.Valid -> pick(parse.pattern)
        }

    private fun pick(textPattern: TextPattern): Parsed<PatternPick> =
        when (field) {
            CHANNEL -> Parsed.Ok(PatternPick.Channel(textPattern))
            CHAT_NAME -> Parsed.Ok(PatternPick.ChatName(textPattern))
            else -> Parsed.Failed("unknown pattern field \"$field\"")
        }
}
