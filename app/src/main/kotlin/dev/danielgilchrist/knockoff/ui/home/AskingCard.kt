package dev.danielgilchrist.knockoff.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun AskingCard(
    onAllowNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Asking about new chats", style = MaterialTheme.typography.titleMedium)
            Text(
                text =
                    "A rule asks about new chats, but Knockoff can't post notifications. New chats still appear " +
                        "below.",
            )
            Button(onClick = onAllowNotifications, modifier = Modifier.padding(top = 8.dp)) {
                Text(text = "Allow notifications")
            }
        }
    }
}
