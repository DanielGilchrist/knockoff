package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun BreakEditor(
    number: Int,
    draft: BreakDraft,
    onDraftChange: (BreakDraft) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NumberedCard(title = "Break $number", removeLabel = "Remove this break", onRemove = onRemove, modifier = modifier) {
        OutlinedTextField(
            value = draft.name,
            onValueChange = { onDraftChange(draft.copy(name = it)) },
            label = { Text(text = "Name") },
            singleLine = true,
        )
        WindowFields(window = draft.window, onWindowChange = { onDraftChange(draft.copy(window = it)) })
    }
}
