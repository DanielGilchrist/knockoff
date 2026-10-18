package dev.danielgilchrist.knockoff.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.R

@Composable
internal fun NoticeCard(
    title: String,
    body: String,
    tone: NoticeTone,
    modifier: Modifier = Modifier,
    action: NoticeAction = NoticeAction.None,
) {
    val colors =
        when (tone) {
            NoticeTone.Nudge -> {
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }

            NoticeTone.Problem -> {
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    val icon =
        when (tone) {
            NoticeTone.Nudge -> R.drawable.ic_notifications
            NoticeTone.Problem -> R.drawable.ic_error
        }

    Card(modifier = modifier.fillMaxWidth(), colors = colors) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(painter = painterResource(icon), contentDescription = null)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = body, style = MaterialTheme.typography.bodyMedium)
                when (action) {
                    NoticeAction.None -> {}

                    is NoticeAction.Button -> {
                        FilledTonalButton(
                            onClick = action.onClick,
                            modifier = Modifier.align(Alignment.End).padding(top = 8.dp),
                        ) { Text(text = action.label) }
                    }
                }
            }
        }
    }
}
