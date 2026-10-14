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
import dev.danielgilchrist.knockoff.ui.ListenerAccess

@Composable
internal fun AccessCard(
    access: ListenerAccess,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Notification access", style = MaterialTheme.typography.titleMedium)
            Text(
                text =
                    when (access) {
                        ListenerAccess.Granted -> "Granted. Knockoff can snooze notifications."
                        ListenerAccess.NotGranted -> "Not granted. Knockoff can't see or snooze anything until it is."
                    },
            )
            Button(onClick = onOpenSettings, modifier = Modifier.padding(top = 8.dp)) {
                Text(text = "Open notification access settings")
            }
        }
    }
}
