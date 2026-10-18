package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.domain.rule.Target
import dev.danielgilchrist.knockoff.ui.SeenChatsState
import dev.danielgilchrist.knockoff.ui.TargetChoices
import dev.danielgilchrist.knockoff.ui.components.AddButton
import dev.danielgilchrist.knockoff.ui.components.Caption
import dev.danielgilchrist.knockoff.ui.components.CardRemoval
import dev.danielgilchrist.knockoff.ui.components.ChoiceSegments
import dev.danielgilchrist.knockoff.ui.components.EditCard
import dev.danielgilchrist.knockoff.ui.components.SectionHeader
import dev.danielgilchrist.knockoff.ui.describeCoverage
import dev.danielgilchrist.knockoff.ui.removedAt
import dev.danielgilchrist.knockoff.ui.replacedAt

@Composable
internal fun PatternsEditor(
    choices: TargetChoices,
    patterns: List<PatternDraft>,
    onPatternsChange: (List<PatternDraft>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by rememberSaveable(stateSaver = AppPicking.SAVER) { mutableStateOf<AppPicking>(AppPicking.Closed) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(text = "Patterns")
        Caption(
            text =
                "Match an app's chat names or channel IDs with a regular expression. Case-sensitive; matches " +
                    "anywhere unless anchored with ^ or $.",
        )
        patterns.forEachIndexed { index, pattern ->
            PatternCard(
                number = index + 1,
                pattern = pattern,
                choices = choices,
                onPatternChange = { onPatternsChange(patterns.replacedAt(index, it)) },
                onRemove = { onPatternsChange(patterns.removedAt(index)) },
            )
        }
        AddButton(text = "Add pattern", onClick = { picking = AppPicking.Open(emptySet()) })
    }

    when (val current = picking) {
        AppPicking.Closed -> {}

        is AppPicking.Open -> {
            AppPickerDialog(
                apps = choices.apps,
                selected = current.chosen,
                onSelectedChange = { picking = AppPicking.Open(it) },
                onDismiss = {
                    onPatternsChange(patterns + current.chosen.map { PatternDraft(it, PatternField.ChatName, "") })
                    picking = AppPicking.Closed
                },
            )
        }
    }
}

@Composable
private fun PatternCard(
    number: Int,
    pattern: PatternDraft,
    choices: TargetChoices,
    onPatternChange: (PatternDraft) -> Unit,
    onRemove: () -> Unit,
) {
    EditCard(
        title = "Pattern $number · ${choices.apps.labelOf(pattern.app)}",
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
        )
        Text(text = describe(preview, choices.chats), style = MaterialTheme.typography.bodySmall)
    }
}

private fun describe(
    preview: PatternPreview,
    seen: SeenChatsState,
): String =
    when (preview) {
        PatternPreview.Empty -> "Type a regular expression."
        is PatternPreview.Invalid -> "Not a valid regular expression: ${preview.detail}"
        is PatternPreview.Valid -> describeValid(preview.target, seen)
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
            when (seen) {
                SeenChatsState.Loading -> "Loading chats…"
                is SeenChatsState.Failed -> "Couldn't read the chats seen so far: ${seen.detail}"
                is SeenChatsState.Loaded -> describeCoverage(seen.chats.filter(target::expandsTo).map { it.name.value })
            }
        }
    }
