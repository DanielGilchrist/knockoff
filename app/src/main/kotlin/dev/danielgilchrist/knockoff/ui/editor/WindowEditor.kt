package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.danielgilchrist.knockoff.ui.components.CardRemoval
import dev.danielgilchrist.knockoff.ui.components.EditCard

@Composable
internal fun WindowEditor(
    position: WindowPosition,
    window: WindowDraft,
    onWindowChange: (WindowDraft) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (title, removal) =
        when (position) {
            WindowPosition.Only -> "Hours" to CardRemoval.None
            is WindowPosition.OneOf -> "Hours ${position.number}" to CardRemoval.Remove("Remove these hours", onRemove)
        }

    EditCard(title = title, removal = removal, modifier = modifier) {
        WindowFields(window = window, onWindowChange = onWindowChange)
    }
}
