package dev.danielgilchrist.knockoff.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
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
import dev.danielgilchrist.knockoff.ui.editor.RuleOverviewActions
import dev.danielgilchrist.knockoff.ui.editor.RuleOverviewScreen
import dev.danielgilchrist.knockoff.ui.editor.RuleSectionActions
import dev.danielgilchrist.knockoff.ui.editor.RuleSectionScreen
import dev.danielgilchrist.knockoff.ui.editor.RuleWizardScreen
import dev.danielgilchrist.knockoff.ui.editor.WizardActions
import dev.danielgilchrist.knockoff.ui.home.HomeActions
import dev.danielgilchrist.knockoff.ui.home.HomeScreen
import dev.danielgilchrist.knockoff.ui.home.NewChatsScreen
import dev.danielgilchrist.knockoff.ui.preferences.PreferencesActions
import dev.danielgilchrist.knockoff.ui.preferences.PreferencesScreen

@Composable
fun KnockoffScreen(
    viewModel: MainViewModel,
    system: SystemActions,
    modifier: Modifier = Modifier,
) {
    val setup by viewModel.setup.collectAsStateWithLifecycle()
    val editor = viewModel.editor
    val settings by editor.settings.collectAsStateWithLifecycle()
    val editing by editor.editing.collectAsStateWithLifecycle()
    val choices by viewModel.choices.collectAsStateWithLifecycle()
    val newChats by viewModel.newChats.collectAsStateWithLifecycle()
    val saveStatus by editor.saveStatus.collectAsStateWithLifecycle()
    val preferencesStatus by viewModel.preferences.saveStatus.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf(Screen.Home) }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshSetup()
        onPauseOrDispose {}
    }

    val shown = ShownScreen.of(screen, settings)

    LaunchedEffect(shown) { screen = shown.screen }

    val home =
        homeActions(
            viewModel,
            system,
            onOpenPreferences = { screen = Screen.Preferences },
            onOpenNewChats = { screen = Screen.NewChats },
        )

    Scaffold(modifier = modifier) { padding ->
        when (val current = editing) {
            Editing.Closed -> {
                ClosedScreen(
                    shown = shown,
                    state = ClosedState(setup, settings, saveStatus, preferencesStatus, newChats, choices.apps),
                    home = home,
                    onBackToHome = { screen = Screen.Home },
                    preferences =
                        PreferencesActions(
                            system = system,
                            onDefaultZoneChange = viewModel.preferences::setDefaultZone,
                            onThemeChange = viewModel.preferences::setTheme,
                            onBack = { screen = Screen.Home },
                        ),
                    modifier = Modifier.padding(padding),
                )
            }

            is Editing.Viewing -> {
                RuleOverview(current, settings, choices.apps, saveStatus, editor, Modifier.padding(padding))
            }

            is Editing.Active -> {
                RuleEditor(editing = current, choices = choices, editor = editor, modifier = Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun RuleEditor(
    editing: Editing.Active,
    choices: TargetChoices,
    editor: SettingsEditor,
    modifier: Modifier = Modifier,
) {
    val insetModifier = modifier.consumeWindowInsets(WindowInsets.systemBars)

    when (editing) {
        is Editing.Section -> {
            RuleSectionScreen(
                editing = editing,
                choices = choices,
                actions =
                    RuleSectionActions(
                        onDraftChange = editor::updateDraft,
                        onSave = editor::saveDraft,
                        onCancel = { editor.show(Editing.Viewing(editing.rule)) },
                    ),
                modifier = insetModifier,
            )
        }

        is Editing.Guided -> {
            RuleWizardScreen(
                guided = editing,
                choices = choices,
                actions =
                    WizardActions(
                        onDraftChange = editor::updateDraft,
                        onGuide = editor::guide,
                        onCreate = editor::saveDraft,
                        onCancel = { editor.show(Editing.Closed) },
                    ),
                modifier = insetModifier,
            )
        }
    }
}

private fun homeActions(
    viewModel: MainViewModel,
    system: SystemActions,
    onOpenPreferences: () -> Unit,
    onOpenNewChats: () -> Unit,
): HomeActions =
    HomeActions(
        system = system,
        onOpenPreferences = onOpenPreferences,
        onOpenNewChats = onOpenNewChats,
        onNewRule = { viewModel.editor.show(Editing.Guided.START) },
        onEditRule = { rule -> viewModel.editor.show(Editing.Viewing(rule)) },
        onRuleChange = viewModel.editor::changeRule,
        onLetChatThrough = viewModel::letChatThrough,
        onResetToDefaults = viewModel.editor::resetToDefaults,
    )

@Composable
private fun RuleOverview(
    viewing: Editing.Viewing,
    settings: SettingsState,
    apps: InstalledApps,
    saveStatus: SaveStatus,
    editor: SettingsEditor,
    modifier: Modifier = Modifier,
) {
    val rule =
        when (settings) {
            is SettingsState.Ready -> {
                settings.settings.rules.rules
                    .firstOrNull { it.name == viewing.rule.name }
            }

            SettingsState.Loading, is SettingsState.Failed -> {
                null
            }
        } ?: viewing.rule

    RuleOverviewScreen(
        rule = rule,
        apps = apps,
        saveStatus = saveStatus,
        actions =
            RuleOverviewActions(
                onBack = { editor.show(Editing.Closed) },
                onStateChange = { state -> editor.changeRule(rule.name, RuleChange.SetState(state)) },
                onEditSection = { section -> editor.show(Editing.Section.of(rule, section)) },
                onDelete = { editor.deleteRule(rule.name) },
            ),
        modifier = modifier.consumeWindowInsets(WindowInsets.systemBars),
    )
}

@Composable
private fun ClosedScreen(
    shown: ShownScreen,
    state: ClosedState,
    home: HomeActions,
    onBackToHome: () -> Unit,
    preferences: PreferencesActions,
    modifier: Modifier = Modifier,
) {
    when (shown) {
        ShownScreen.Home -> {
            HomeScreen(
                setup = state.setup,
                settings = state.settings,
                saveStatus = state.saveStatus,
                newChats = state.newChats.chats,
                apps = state.apps,
                actions = home,
                modifier = modifier,
            )
        }

        is ShownScreen.Preferences -> {
            PreferencesScreen(
                settings = shown.settings,
                setup = state.setup,
                saveStatus = state.preferencesStatus,
                actions = preferences,
                modifier = modifier,
            )
        }

        is ShownScreen.NewChats -> {
            NewChatsScreen(
                view = state.newChats,
                saveStatus = state.saveStatus,
                apps = state.apps,
                rules = shown.settings.rules.rules,
                actions = home,
                onBack = onBackToHome,
                modifier = modifier,
            )
        }
    }
}
