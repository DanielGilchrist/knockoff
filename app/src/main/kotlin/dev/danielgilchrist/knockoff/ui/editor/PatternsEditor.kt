package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import dev.danielgilchrist.knockoff.ui.RadioRow
import dev.danielgilchrist.knockoff.ui.SeenChatsState
import dev.danielgilchrist.knockoff.ui.TargetChoices
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
        Text(text = "Patterns", style = MaterialTheme.typography.titleSmall)
        Text(
            text =
                "For power users: pick an app's notifications whose chat name or channel ID matches a regular " +
                    "expression. It matches anywhere unless anchored with ^ or $, and is case-sensitive. Chats " +
                    "not seen yet are matched by their notification title.",
            style = MaterialTheme.typography.bodySmall,
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
        OutlinedButton(onClick = { picking = AppPicking.Open(emptySet()) }) { Text(text = "Add pattern") }
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
    NumberedCard(
        title = "Pattern $number · ${choices.apps.labelOf(pattern.app)}",
        removeLabel = "Remove this pattern",
        onRemove = onRemove,
    ) {
        RadioRow(
            text = "Chat name",
            selected = pattern.field == PatternField.ChatName,
            onClick = { onPatternChange(pattern.copy(field = PatternField.ChatName)) },
        )
        RadioRow(
            text = "Channel ID",
            selected = pattern.field == PatternField.ChannelId,
            onClick = { onPatternChange(pattern.copy(field = PatternField.ChannelId)) },
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
