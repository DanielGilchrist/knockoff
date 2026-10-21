package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import dev.danielgilchrist.knockoff.ui.Editing
import dev.danielgilchrist.knockoff.ui.components.supportingText
import dev.danielgilchrist.knockoff.ui.describe

@Composable
internal fun RenameDialog(
    renaming: Editing.Renaming,
    actions: RuleSectionActions,
    modifier: Modifier = Modifier,
) {
    val draft = renaming.draft
    val problems = renaming.problems
    val focus = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = actions.onCancel,
        modifier = modifier,
        title = { Text(text = "Rename rule") },
        text = {
            LaunchedEffect(Unit) { focus.requestFocus() }
            OutlinedTextField(
                value = draft.name,
                onValueChange = { actions.onDraftChange(draft.copy(name = it)) },
                label = { Text(text = "Name") },
                singleLine = true,
                isError = problems.isNotEmpty(),
                supportingText =
                    supportingText(
                        problems.joinToString(" ") { problem -> problem.describe({ it.value }, { it.value }) },
                    ),
                keyboardOptions =
                    KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { actions.onSave() }),
                modifier = Modifier.focusRequester(focus),
            )
        },
        confirmButton = { TextButton(onClick = actions.onSave) { Text(text = "Save") } },
        dismissButton = { TextButton(onClick = actions.onCancel) { Text(text = "Cancel") } },
    )
}
