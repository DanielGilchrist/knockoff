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
import dev.danielgilchrist.knockoff.ui.DeviceLink
import dev.danielgilchrist.knockoff.ui.ExactAlarms

@Composable
internal fun SilentHoldsCard(
    link: DeviceLink,
    alarms: ExactAlarms,
    onLinkDevice: () -> Unit,
    onAllowExactAlarms: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Silent holds", style = MaterialTheme.typography.titleMedium)
            when (link) {
                DeviceLink.NotLinked -> {
                    Text(
                        text =
                            "Not linked. Held notifications still buzz once before they're snoozed. Android only " +
                                "lets an app silence other apps once it's linked to a device such as a watch.",
                    )
                    Button(onClick = onLinkDevice, modifier = Modifier.padding(top = 8.dp)) {
                        Text(text = "Link a device")
                    }
                }

                DeviceLink.Linked -> {
                    Text(text = "Linked. Held apps are silenced, so they don't buzz before they're snoozed.")
                    ExactAlarmsSection(alarms = alarms, onAllowExactAlarms = onAllowExactAlarms)
                }
            }
        }
    }
}

@Composable
private fun ExactAlarmsSection(
    alarms: ExactAlarms,
    onAllowExactAlarms: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(top = 8.dp)) {
        when (alarms) {
            ExactAlarms.Allowed -> {
                Text(text = "Silencing starts and ends on time.")
            }

            ExactAlarms.NotAllowed -> {
                Text(text = "Silencing may start or end a few minutes late until exact alarms are allowed.")
                Button(onClick = onAllowExactAlarms, modifier = Modifier.padding(top = 8.dp)) {
                    Text(text = "Allow exact alarms")
                }
            }
        }
    }
}
