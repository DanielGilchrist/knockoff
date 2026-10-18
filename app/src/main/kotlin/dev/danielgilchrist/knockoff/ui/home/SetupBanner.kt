package dev.danielgilchrist.knockoff.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.ui.SetupNotice
import dev.danielgilchrist.knockoff.ui.SystemActions

@Composable
internal fun SetupBanner(
    notice: SetupNotice,
    system: SystemActions,
    modifier: Modifier = Modifier,
) {
    when (notice) {
        SetupNotice.Clear -> {}

        SetupNotice.NoAccess -> {
            Banner(
                text = "Knockoff can't see notifications yet, so no rule holds anything.",
                action = "Grant access",
                onAction = system.onOpenAccessSettings,
                modifier = modifier,
            )
        }

        SetupNotice.NoNotifications -> {
            Banner(
                text = "A rule asks about new chats, but Knockoff isn't allowed to post notifications.",
                action = "Allow notifications",
                onAction = system.onAllowNotifications,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun Banner(
    text: String,
    action: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = text)
            Button(onClick = onAction, modifier = Modifier.padding(top = 8.dp)) { Text(text = action) }
        }
    }
}
