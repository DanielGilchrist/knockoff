package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.danielgilchrist.knockoff.ui.components.CardRemoval
import dev.danielgilchrist.knockoff.ui.components.EditCard

@Composable
internal fun BreakEditor(
    number: Int,
    draft: BreakDraft,
    onDraftChange: (BreakDraft) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EditCard(
        title = "Break $number",
        removal = CardRemoval.Remove("Remove this break", onRemove),
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = draft.name,
            onValueChange = { onDraftChange(draft.copy(name = it)) },
            label = { Text(text = "Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        WindowFields(window = draft.window, onWindowChange = { onDraftChange(draft.copy(window = it)) })
    }
}
