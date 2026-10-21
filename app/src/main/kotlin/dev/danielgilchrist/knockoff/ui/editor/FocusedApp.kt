package dev.danielgilchrist.knockoff.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.RuleApps
import dev.danielgilchrist.knockoff.ui.AppFocus
import dev.danielgilchrist.knockoff.ui.TargetChoices

@Composable
internal fun FocusedApp(
    app: PackageName,
    draft: RuleDraft,
    stored: RuleApps,
    choices: TargetChoices,
    onDraftChange: (RuleDraft) -> Unit,
    onFocus: (AppFocus) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (val appDraft = draft.apps[app]) {
        null -> {
            val backToList by rememberUpdatedState(onFocus)

            LaunchedEffect(app) { backToList(AppFocus.List) }
        }

        else -> {
            AppScreen(
                app = app,
                draft = appDraft,
                rule = draft,
                stored = stored,
                choices = choices,
                actions =
                    AppScreenActions(
                        onDraftChange = { onDraftChange(draft.withApp(app, it)) },
                        onRemove = {
                            onDraftChange(draft.withoutApp(app))
                            onFocus(AppFocus.List)
                        },
                        onBack = { onFocus(AppFocus.List) },
                    ),
                modifier = modifier,
            )
        }
    }
}
