package dev.danielgilchrist.knockoff.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.R
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.ui.InstalledApps
import dev.danielgilchrist.knockoff.ui.RuleOverview
import dev.danielgilchrist.knockoff.ui.RuleSection
import dev.danielgilchrist.knockoff.ui.SaveStatus
import dev.danielgilchrist.knockoff.ui.components.DestructiveMenuItem
import dev.danielgilchrist.knockoff.ui.components.ListGroup
import dev.danielgilchrist.knockoff.ui.components.ListScreen
import dev.danielgilchrist.knockoff.ui.components.MainSwitchBar
import dev.danielgilchrist.knockoff.ui.components.NavigationListItem
import dev.danielgilchrist.knockoff.ui.components.OverflowMenu
import dev.danielgilchrist.knockoff.ui.components.ScreenNavigation
import dev.danielgilchrist.knockoff.ui.saveProblem

@Composable
internal fun RuleOverviewScreen(
    rule: Rule,
    apps: InstalledApps,
    saveStatus: SaveStatus,
    actions: RuleOverviewActions,
    modifier: Modifier = Modifier,
    overlay: @Composable () -> Unit = {},
) {
    val overview = RuleOverview.of(rule, apps::labelOf)
    var deleting by rememberSaveable { mutableStateOf(false) }

    BackHandler(onBack = actions.onBack)

    ListScreen(
        title = rule.name.value,
        navigation = ScreenNavigation.Back(actions.onBack),
        modifier = modifier,
        actions = {
            RuleMenu(
                onRename = actions.onRename,
                onDelete = { deleting = true },
            )
        },
    ) {
        item {
            MainSwitchBar(text = "Use this rule", state = rule.state, onStateChange = actions.onStateChange)
        }
        item {
            Text(
                text = overview.effect,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
        }
        saveProblem(saveStatus)
        item { SectionRows(overview = overview, onEditSection = actions.onEditSection) }
    }

    overlay()
    if (deleting) {
        DeleteDialog(
            name = rule.name.value,
            onConfirm = {
                deleting = false
                actions.onDelete()
            },
            onDismiss = { deleting = false },
        )
    }
}

@Composable
private fun SectionRows(
    overview: RuleOverview,
    onEditSection: (RuleSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    ListGroup(modifier = modifier) {
        NavigationListItem(
            icon = R.drawable.ic_apps,
            headline = "Apps",
            supporting = overview.apps,
            onClick = { onEditSection(RuleSection.Apps) },
        )
        NavigationListItem(
            icon = R.drawable.ic_schedule,
            headline = "Hours",
            supporting = overview.hours,
            onClick = { onEditSection(RuleSection.Hours) },
        )
    }
}

@Composable
private fun RuleMenu(
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    OverflowMenu { close ->
        DropdownMenuItem(
            text = { Text(text = "Rename") },
            leadingIcon = { Icon(painter = painterResource(R.drawable.ic_edit), contentDescription = null) },
            onClick = {
                close()
                onRename()
            },
        )
        DestructiveMenuItem(
            text = "Delete rule",
            onClick = {
                close()
                onDelete()
            },
        )
    }
}

@Composable
private fun DeleteDialog(
    name: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(painter = painterResource(R.drawable.ic_delete), contentDescription = null) },
        title = { Text(text = "Delete $name?") },
        text = { Text(text = "Its notifications stop being held. This can't be undone.") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(text = "Delete") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(text = "Cancel") } },
    )
}
