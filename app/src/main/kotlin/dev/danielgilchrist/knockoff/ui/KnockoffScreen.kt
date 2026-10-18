package dev.danielgilchrist.knockoff.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.ui.editor.RenameDialog
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

    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
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
                )
            }

            is Editing.Viewing -> {
                RuleOverview(current.rule, settings, choices.apps, saveStatus, editor)
            }

            is Editing.Renaming -> {
                RuleOverview(current.rule, settings, choices.apps, saveStatus, editor) {
                    RenameDialog(renaming = current, actions = sectionActions(editor, current.rule))
                }
            }

            is Editing.Section -> {
                SectionEditor(editing = current, choices = choices, editor = editor)
            }

            is Editing.Guided -> {
                Wizard(guided = current, choices = choices, editor = editor)
            }
        }
    }
}

@Composable
private fun SectionEditor(
    editing: Editing.Section,
    choices: TargetChoices,
    editor: SettingsEditor,
    modifier: Modifier = Modifier,
) {
    RuleSectionScreen(
        editing = editing,
        choices = choices,
        actions = sectionActions(editor, editing.rule),
        modifier = modifier,
    )
}

@Composable
private fun Wizard(
    guided: Editing.Guided,
    choices: TargetChoices,
    editor: SettingsEditor,
    modifier: Modifier = Modifier,
) {
    RuleWizardScreen(
        guided = guided,
        choices = choices,
        actions =
            WizardActions(
                onDraftChange = editor::updateDraft,
                onGuide = editor::guide,
                onCreate = editor::saveDraft,
                onCancel = { editor.show(Editing.Closed) },
            ),
        modifier = modifier,
    )
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
    shown: Rule,
    settings: SettingsState,
    apps: InstalledApps,
    saveStatus: SaveStatus,
    editor: SettingsEditor,
    modifier: Modifier = Modifier,
    overlay: @Composable () -> Unit = {},
) {
    val rule =
        when (settings) {
            is SettingsState.Ready -> {
                settings.settings.rules.rules
                    .firstOrNull { it.name == shown.name }
            }

            SettingsState.Loading, is SettingsState.Failed -> {
                null
            }
        } ?: shown

    RuleOverviewScreen(
        rule = rule,
        apps = apps,
        saveStatus = saveStatus,
        actions =
            RuleOverviewActions(
                onBack = { editor.show(Editing.Closed) },
                onStateChange = { state -> editor.changeRule(rule.name, RuleChange.SetState(state)) },
                onEditSection = { section -> editor.show(Editing.Section.of(rule, section)) },
                onRename = { editor.show(Editing.Renaming.of(rule)) },
                onDelete = { editor.deleteRule(rule.name) },
            ),
        modifier = modifier,
        overlay = overlay,
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

private fun sectionActions(
    editor: SettingsEditor,
    rule: Rule,
): RuleSectionActions =
    RuleSectionActions(
        onDraftChange = editor::updateDraft,
        onSave = editor::saveDraft,
        onCancel = { editor.show(Editing.Viewing(rule)) },
    )
