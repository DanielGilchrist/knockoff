package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.R
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.ui.InstalledApps
import dev.danielgilchrist.knockoff.ui.components.AddButton
import dev.danielgilchrist.knockoff.ui.components.CategoryCard
import dev.danielgilchrist.knockoff.ui.components.RemovableChip

@Composable
internal fun AppsEditor(
    apps: InstalledApps,
    selected: Set<PackageName>,
    onSelectedChange: (Set<PackageName>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by rememberSaveable { mutableStateOf(false) }

    CategoryCard(icon = R.drawable.ic_apps, title = "Apps", count = selected.size, modifier = modifier) {
        if (selected.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                selected.sortedBy { apps.labelOf(it) }.forEach { packageName ->
                    RemovableChip(label = apps.labelOf(packageName), onRemove = {
                        onSelectedChange(selected - packageName)
                    })
                }
            }
        }
        AddButton(text = "Choose apps", onClick = { picking = true })
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
