package dev.danielgilchrist.knockoff.ui.preferences

import androidx.compose.foundation.clickable
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import dev.danielgilchrist.knockoff.R
import dev.danielgilchrist.knockoff.ui.components.IconBadge
import dev.danielgilchrist.knockoff.ui.components.groupedItemColors

@Composable
internal fun SetupRow(
    item: SetupItem,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = { Text(text = item.action.headline) },
        supportingContent = { Text(text = item.supporting) },
        leadingContent = { IconBadge(icon = item.action.icon) },
        trailingContent = {
            when (val status = item.status) {
                SetupStatus.Done -> {
                    Icon(
                        painter = painterResource(R.drawable.ic_check_circle),
                        contentDescription = "Done",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }

                SetupStatus.Explained -> {
                    Icon(painter = painterResource(R.drawable.ic_chevron), contentDescription = null)
                }

                is SetupStatus.Needed -> {
                    FilledTonalButton(onClick = onAction) { Text(text = status.label) }
                }
            }
        },
        colors = groupedItemColors(),
        modifier =
            when (item.status) {
                SetupStatus.Explained -> {
                    modifier.clickable(
                        onClickLabel = "Open",
                        role = Role.Button,
                        onClick = onAction,
                    )
                }

                SetupStatus.Done, is SetupStatus.Needed -> {
                    modifier
                }
            },
    )
}
