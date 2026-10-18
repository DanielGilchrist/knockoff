package dev.danielgilchrist.knockoff.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

internal fun supportingText(text: String): (@Composable () -> Unit)? =
    when {
        text.isEmpty() -> null
        else -> ({ Text(text = text) })
    }
