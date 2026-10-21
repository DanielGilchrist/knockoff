package dev.danielgilchrist.knockoff.ui.components

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import dev.danielgilchrist.knockoff.R

@Composable
internal fun DestructiveMenuItem(
    text: String,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(text = text) },
        leadingIcon = { Icon(painter = painterResource(R.drawable.ic_delete), contentDescription = null) },
        colors =
            MenuDefaults.itemColors(
                textColor = MaterialTheme.colorScheme.error,
                leadingIconColor = MaterialTheme.colorScheme.error,
            ),
        onClick = onClick,
    )
}
