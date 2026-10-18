package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import dev.danielgilchrist.knockoff.R

@Composable
internal fun SectionRow(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(text = title) },
        supportingContent = { Text(text = value) },
        trailingContent = { Icon(painter = painterResource(R.drawable.ic_chevron), contentDescription = null) },
        modifier = modifier.clickable(onClick = onClick),
    )
}
