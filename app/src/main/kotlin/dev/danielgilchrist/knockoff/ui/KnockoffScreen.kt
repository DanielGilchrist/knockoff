package dev.danielgilchrist.knockoff.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.danielgilchrist.knockoff.ui.editor.RuleEditorActions
import dev.danielgilchrist.knockoff.ui.editor.RuleEditorScreen
import dev.danielgilchrist.knockoff.ui.home.HomeActions
import dev.danielgilchrist.knockoff.ui.home.HomeScreen

@Composable
fun KnockoffScreen(
    viewModel: MainViewModel,
    onOpenAccessSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val access by viewModel.access.collectAsStateWithLifecycle()
    val editor = viewModel.editor
    val settings by editor.settings.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val editing by editor.editing.collectAsStateWithLifecycle()
    val saveStatus by editor.saveStatus.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refreshAccess()
        onPauseOrDispose {}
    }

    Scaffold(modifier = modifier) { padding ->
        when (val current = editing) {
            Editing.Closed -> {
                HomeScreen(
                    access = access,
                    settings = settings,
                    saveStatus = saveStatus,
                    actions =
                        HomeActions(
                            onOpenAccessSettings = onOpenAccessSettings,
                            onNewRule = { editor.startEditing(EditTarget.New) },
                            onEditRule = { rule -> editor.startEditing(EditTarget.Existing(rule)) },
                            onRuleStateChange = editor::setRuleState,
                            onResetToDefaults = editor::resetToDefaults,
                        ),
                    modifier = Modifier.padding(padding),
                )
            }

            is Editing.Open -> {
                RuleEditorScreen(
                    editing = current,
                    apps = apps,
                    actions =
                        RuleEditorActions(
                            onDraftChange = editor::updateDraft,
                            onSave = editor::saveDraft,
                            onCancel = editor::closeEditor,
                            onDelete = { target -> editor.deleteRule(target.rule.name) },
                        ),
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}
