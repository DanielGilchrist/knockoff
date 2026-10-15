package dev.danielgilchrist.knockoff.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.danielgilchrist.knockoff.ui.editor.RuleEditorActions
import dev.danielgilchrist.knockoff.ui.editor.RuleEditorScreen
import dev.danielgilchrist.knockoff.ui.home.HomeActions
import dev.danielgilchrist.knockoff.ui.home.HomeScreen
import dev.danielgilchrist.knockoff.ui.preferences.PreferencesActions
import dev.danielgilchrist.knockoff.ui.preferences.PreferencesScreen

@Composable
fun KnockoffScreen(
    viewModel: MainViewModel,
    onOpenAccessSettings: () -> Unit,
    onLinkDevice: () -> Unit,
    onAllowExactAlarms: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val setup by viewModel.setup.collectAsStateWithLifecycle()
    val editor = viewModel.editor
    val settings by editor.settings.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val editing by editor.editing.collectAsStateWithLifecycle()
    val saveStatus by editor.saveStatus.collectAsStateWithLifecycle()
    val preferencesStatus by viewModel.preferences.saveStatus.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf(Screen.Home) }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshSetup()
        onPauseOrDispose {}
    }

    val shown = ShownScreen.of(screen, settings)

    LaunchedEffect(shown) { screen = shown.screen }

    Scaffold(modifier = modifier) { padding ->
        when (val current = editing) {
            Editing.Closed -> {
                when (shown) {
                    ShownScreen.Home -> {
                        HomeScreen(
                            setup = setup,
                            settings = settings,
                            saveStatus = saveStatus,
                            actions =
                                HomeActions(
                                    onOpenAccessSettings = onOpenAccessSettings,
                                    onOpenPreferences = { screen = Screen.Preferences },
                                    onLinkDevice = onLinkDevice,
                                    onAllowExactAlarms = onAllowExactAlarms,
                                    onNewRule = { editor.startEditing(EditTarget.New) },
                                    onEditRule = { rule -> editor.startEditing(EditTarget.Existing(rule)) },
                                    onRuleStateChange = editor::setRuleState,
                                    onResetToDefaults = editor::resetToDefaults,
                                ),
                            modifier = Modifier.padding(padding),
                        )
                    }

                    is ShownScreen.Preferences -> {
                        PreferencesScreen(
                            settings = shown.settings,
                            saveStatus = preferencesStatus,
                            actions =
                                PreferencesActions(
                                    onDefaultZoneChange = viewModel.preferences::setDefaultZone,
                                    onThemeChange = viewModel.preferences::setTheme,
                                    onBack = { screen = Screen.Home },
                                ),
                            modifier = Modifier.padding(padding),
                        )
                    }
                }
            }

            is Editing.Open -> {
                RuleEditor(editing = current, apps = apps, editor = editor, modifier = Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun RuleEditor(
    editing: Editing.Open,
    apps: InstalledApps,
    editor: SettingsEditor,
    modifier: Modifier = Modifier,
) {
    RuleEditorScreen(
        editing = editing,
        apps = apps,
        actions =
            RuleEditorActions(
                onDraftChange = editor::updateDraft,
                onSave = editor::saveDraft,
                onCancel = editor::closeEditor,
                onDelete = { target -> editor.deleteRule(target.rule.name) },
            ),
        modifier = modifier,
    )
}
