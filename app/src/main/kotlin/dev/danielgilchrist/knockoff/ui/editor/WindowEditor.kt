package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun WindowEditor(
    number: Int,
    window: WindowDraft,
    onWindowChange: (WindowDraft) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NumberedCard(
        title = "Hours $number",
        removeLabel = "Remove these hours",
        onRemove = onRemove,
        modifier = modifier,
    ) {
        WindowFields(window = window, onWindowChange = onWindowChange)
    }
}
