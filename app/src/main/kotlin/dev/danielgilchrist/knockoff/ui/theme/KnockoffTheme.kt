package dev.danielgilchrist.knockoff.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import dev.danielgilchrist.knockoff.domain.settings.Theme

@Composable
fun KnockoffTheme(
    dark: Boolean,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colours =
        when {
            dark -> dynamicDarkColorScheme(context)
            else -> dynamicLightColorScheme(context)
        }

    MaterialTheme(colorScheme = colours, content = content)
}

@Composable
fun Theme.isDark(): Boolean =
    when (this) {
        Theme.System -> isSystemInDarkTheme()
        Theme.Light -> false
        Theme.Dark -> true
    }
