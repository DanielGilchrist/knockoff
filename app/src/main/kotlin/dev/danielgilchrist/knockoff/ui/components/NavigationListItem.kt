package dev.danielgilchrist.knockoff.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import dev.danielgilchrist.knockoff.R

@Composable
internal fun NavigationListItem(
    @DrawableRes icon: Int,
    headline: String,
    supporting: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: SupportingTone = SupportingTone.Plain,
) {
    ListItem(
        headlineContent = { Text(text = headline) },
        supportingContent = {
            Text(
                text = supporting,
                color =
                    when (tone) {
                        SupportingTone.Plain -> Color.Unspecified
                        SupportingTone.Problem -> MaterialTheme.colorScheme.error
                    },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = { IconBadge(icon = icon) },
        trailingContent = { Icon(painter = painterResource(R.drawable.ic_chevron), contentDescription = null) },
        colors = groupedItemColors(),
        modifier = modifier.clickable(onClick = onClick),
    )
}
