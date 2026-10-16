package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.ui.InstalledApps

@Composable
internal fun AppsEditor(
    apps: InstalledApps,
    selected: Set<PackageName>,
    onSelectedChange: (Set<PackageName>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(text = "Apps", style = MaterialTheme.typography.titleSmall)
        selected.forEach { packageName ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = apps.labelOf(packageName), modifier = Modifier.weight(1f))
                TextButton(onClick = { onSelectedChange(selected - packageName) }) { Text(text = "Remove") }
            }
        }
        OutlinedButton(onClick = { picking = true }) { Text(text = "Choose apps") }
    }

    if (picking) {
        AppPickerDialog(
            apps = apps,
            selected = selected,
            onSelectedChange = onSelectedChange,
            onDismiss = { picking = false },
        )
    }
}
