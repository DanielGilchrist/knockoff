package dev.danielgilchrist.knockoff.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun SettingsProblemCard(
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit = {},
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = detail)
            action()
        }
    }
}

internal fun LazyListScope.saveProblem(
    saveStatus: SaveStatus,
    modifier: Modifier = Modifier,
) {
    when (saveStatus) {
        SaveStatus.Idle -> {}

        is SaveStatus.Failed -> {
            item {
                SettingsProblemCard(
                    title = "Couldn't save settings",
                    detail = saveStatus.detail,
                    modifier = modifier,
                )
            }
        }
    }
}
