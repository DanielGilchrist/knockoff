package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.PatternPick
import dev.danielgilchrist.knockoff.domain.rule.TextPattern
import dev.danielgilchrist.knockoff.domain.rule.TextPatternParse

data class PatternDraft(
    val field: PatternField,
    val source: String,
) {
    companion object {
        val BLANK: PatternDraft get() = PatternDraft(PatternField.ChatName, "")

        fun from(pick: PatternPick): PatternDraft =
            PatternDraft(
                when (pick) {
                    is PatternPick.ChatName -> PatternField.ChatName
                    is PatternPick.Channel -> PatternField.ChannelId
                },
                pick.pattern.source,
            )
    }

    fun parse(
        app: PackageName,
        number: Int,
    ): Checked<PatternPick> =
        when (val preview = preview()) {
            PatternPreview.Empty -> {
                Checked.Problems(listOf(DraftProblem.BlankPattern(app, number)))
            }

            is PatternPreview.Invalid -> {
                Checked.Problems(
                    listOf(DraftProblem.InvalidPattern(app, number, preview.detail)),
                )
            }

            is PatternPreview.Valid -> {
                Checked.Ok(preview.pick)
            }
        }

    fun preview(): PatternPreview =
        when (val parse = TextPattern.parse(source)) {
            TextPatternParse.Blank -> PatternPreview.Empty
            is TextPatternParse.Invalid -> PatternPreview.Invalid(parse.detail)
            is TextPatternParse.Valid -> PatternPreview.Valid(pick(parse.pattern))
        }

    private fun pick(pattern: TextPattern): PatternPick =
        when (field) {
            PatternField.ChatName -> PatternPick.ChatName(pattern)
            PatternField.ChannelId -> PatternPick.Channel(pattern)
        }
}
