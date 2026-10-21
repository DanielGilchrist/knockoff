package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.danielgilchrist.knockoff.R
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.RuleApps
import dev.danielgilchrist.knockoff.ui.TargetChoices
import dev.danielgilchrist.knockoff.ui.components.AddButton
import dev.danielgilchrist.knockoff.ui.components.Caption
import dev.danielgilchrist.knockoff.ui.components.ListGroup
import dev.danielgilchrist.knockoff.ui.components.NavigationListItem

@Composable
internal fun AppsList(
    draft: RuleDraft,
    stored: RuleApps,
    choices: TargetChoices,
    onDraftChange: (RuleDraft) -> Unit,
    onOpenApp: (PackageName) -> Unit,
    use: AppsListUse,
    modifier: Modifier = Modifier,
) {
    var picking by rememberSaveable(stateSaver = AppPicking.SAVER) { mutableStateOf<AppPicking>(AppPicking.Closed) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Caption(text = use.caption(draft))
        when {
            draft.apps.isEmpty() -> {
                if (use.saysWhenEmpty) Caption(text = "No apps yet.")
            }

            else -> {
                ListGroup {
                    draft.apps.forEach { (app, appDraft) -> AppRow(app, appDraft, stored, choices, onOpenApp) }
                }
            }
        }
        AddButton(text = "Add apps", onClick = { picking = AppPicking.Open(emptySet()) })
    }

    when (val current = picking) {
        AppPicking.Closed -> {}

        is AppPicking.Open -> {
            AppPickerDialog(
                apps = choices.apps,
                added = draft.apps.keys,
                selected = current.chosen,
                onSelectedChange = { picking = AppPicking.Open(it) },
                onAdd = {
                    onDraftChange(draft.withApps(current.chosen))
                    picking = AppPicking.Closed
                },
                onClose = { picking = AppPicking.Closed },
            )
        }
    }
}

@Composable
private fun AppRow(
    app: PackageName,
    draft: AppDraft,
    stored: RuleApps,
    choices: TargetChoices,
    onOpenApp: (PackageName) -> Unit,
) {
    val support = AppRowSupport.of(app, choices.apps.isMissing(app), draft, AppKind.forApp(app, draft, stored, choices))

    NavigationListItem(
        icon = R.drawable.ic_apps,
        headline = choices.apps.labelOf(app),
        supporting = support.text,
        onClick = { onOpenApp(app) },
        tone = support.tone,
    )
}
