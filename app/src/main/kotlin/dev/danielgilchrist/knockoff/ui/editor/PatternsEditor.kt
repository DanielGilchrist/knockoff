package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.Target
import dev.danielgilchrist.knockoff.ui.SeenChatsState
import dev.danielgilchrist.knockoff.ui.components.AddButton
import dev.danielgilchrist.knockoff.ui.components.Caption
import dev.danielgilchrist.knockoff.ui.components.CardRemoval
import dev.danielgilchrist.knockoff.ui.components.ChoiceSegments
import dev.danielgilchrist.knockoff.ui.components.EditCard
import dev.danielgilchrist.knockoff.ui.components.SectionHeader
import dev.danielgilchrist.knockoff.ui.describe
import dev.danielgilchrist.knockoff.ui.describeCoverage
import dev.danielgilchrist.knockoff.ui.removedAt
import dev.danielgilchrist.knockoff.ui.replacedAt

@Composable
internal fun PatternsEditor(
    app: PackageName,
    seen: SeenChatsState,
    patterns: List<PatternDraft>,
    matchesChats: Boolean,
    onPatternsChange: (List<PatternDraft>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(text = "Patterns")
        Caption(
            text =
                when {
                    matchesChats -> "Match chat names with a regular expression."
                    else -> "Match notification channel IDs with a regular expression."
                },
        )
        patterns.forEachIndexed { index, pattern ->
            PatternCard(
                number = index + 1,
                pattern = pattern,
                app = app,
                seen = seen,
                onPatternChange = { onPatternsChange(patterns.replacedAt(index, it)) },
                onRemove = { onPatternsChange(patterns.removedAt(index)) },
            )
        }
        AddButton(text = "Add a pattern", onClick = { onPatternsChange(patterns + PatternDraft.BLANK) })
    }
}

@Composable
private fun PatternCard(
    number: Int,
    pattern: PatternDraft,
    app: PackageName,
    seen: SeenChatsState,
    onPatternChange: (PatternDraft) -> Unit,
    onRemove: () -> Unit,
) {
    EditCard(
        title = "Pattern $number",
        removal = CardRemoval.Remove("Remove this pattern", onRemove),
    ) {
        ChoiceSegments(
            options = PatternField.entries,
            selected = pattern.field,
            label = {
                when (it) {
                    PatternField.ChatName -> "Chat name"
                    PatternField.ChannelId -> "Channel ID"
                }
            },
            onSelect = { onPatternChange(pattern.copy(field = it)) },
        )
        val preview = pattern.preview()
        OutlinedTextField(
            value = pattern.source,
            onValueChange = { onPatternChange(pattern.copy(source = it)) },
            label = { Text(text = "Regular expression") },
            singleLine = true,
            isError = preview is PatternPreview.Invalid,
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
            supportingText = { Text(text = describe(preview, app, seen)) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun describe(
    preview: PatternPreview,
    app: PackageName,
    seen: SeenChatsState,
): String =
    when (preview) {
        PatternPreview.Empty -> "Case-sensitive. Matches anywhere unless you use ^ or $."
        is PatternPreview.Invalid -> "Not a valid regular expression: ${preview.detail}"
        is PatternPreview.Valid -> describeValid(preview.pick.inApp(app), seen)
    }

private fun describeValid(
    target: Target.Pattern,
    seen: SeenChatsState,
): String =
    when (target) {
        is Target.ChannelPattern -> {
            "Matches the app's notification channels by their ID."
        }

        is Target.ChatNamePattern -> {
            seen.describe { chats -> describeCoverage(chats.filter(target::expandsTo).map { it.name.value }) }
        }
    }
