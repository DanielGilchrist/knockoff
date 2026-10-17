package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.Target
import dev.danielgilchrist.knockoff.domain.rule.TextPattern
import dev.danielgilchrist.knockoff.domain.rule.TextPatternParse

data class PatternDraft(
    val app: PackageName,
    val field: PatternField,
    val source: String,
) {
    companion object {
        fun from(target: Target.Pattern): PatternDraft =
            PatternDraft(
                target.packageName,
                when (target) {
                    is Target.ChatNamePattern -> PatternField.ChatName
                    is Target.ChannelPattern -> PatternField.ChannelId
                },
                target.pattern.source,
            )
    }

    fun parse(number: Int): Checked<Target.Pattern> =
        when (val preview = preview()) {
            PatternPreview.Empty -> Checked.Problems(listOf(DraftProblem.BlankPattern(number)))
            is PatternPreview.Invalid -> Checked.Problems(listOf(DraftProblem.InvalidPattern(number, preview.detail)))
            is PatternPreview.Valid -> Checked.Ok(preview.target)
        }

    fun preview(): PatternPreview =
        when (val parse = TextPattern.parse(source)) {
            TextPatternParse.Blank -> PatternPreview.Empty
            is TextPatternParse.Invalid -> PatternPreview.Invalid(parse.detail)
            is TextPatternParse.Valid -> PatternPreview.Valid(target(parse.pattern))
        }

    private fun target(pattern: TextPattern): Target.Pattern =
        when (field) {
            PatternField.ChatName -> Target.ChatNamePattern(app, pattern)
            PatternField.ChannelId -> Target.ChannelPattern(app, pattern)
        }
}
