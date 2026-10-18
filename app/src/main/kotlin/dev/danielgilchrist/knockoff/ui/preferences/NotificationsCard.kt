package dev.danielgilchrist.knockoff.ui.preferences

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
import dev.danielgilchrist.knockoff.ui.NotificationPermission

@Composable
internal fun NotificationsCard(
    permission: NotificationPermission,
    onAllowNotifications: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Knockoff's own notifications", style = MaterialTheme.typography.titleMedium)
            when (permission) {
                NotificationPermission.Granted -> {
                    Text(text = "Allowed. Rules that ask about new chats can post a quiet notification.")
                }

                NotificationPermission.NotGranted -> {
                    Text(text = "Not allowed. Rules that ask about new chats can only list them in Knockoff.")
                    Button(onClick = onAllowNotifications, modifier = Modifier.padding(top = 8.dp)) {
                        Text(text = "Allow notifications")
                    }
                }
            }
        }
    }
}
