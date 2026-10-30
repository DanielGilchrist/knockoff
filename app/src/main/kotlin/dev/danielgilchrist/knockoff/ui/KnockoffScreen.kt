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
import dev.danielgilchrist.knockoff.domain.group.KnownGroups
import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.ui.editor.DayOffActions
import dev.danielgilchrist.knockoff.ui.editor.RenameDialog
import dev.danielgilchrist.knockoff.ui.editor.RuleOverviewActions
import dev.danielgilchrist.knockoff.ui.editor.RuleOverviewScreen
import dev.danielgilchrist.knockoff.ui.editor.RuleSectionActions
import dev.danielgilchrist.knockoff.ui.editor.RuleSectionScreen
import dev.danielgilchrist.knockoff.ui.editor.RuleWizardScreen
import dev.danielgilchrist.knockoff.ui.editor.SectionFacts
import dev.danielgilchrist.knockoff.ui.editor.WizardActions
import dev.danielgilchrist.knockoff.ui.home.HomeActions
import dev.danielgilchrist.knockoff.ui.home.HomeScreen
import dev.danielgilchrist.knockoff.ui.home.NewChatsScreen
import dev.danielgilchrist.knockoff.ui.preferences.HolidaysActions
import dev.danielgilchrist.knockoff.ui.preferences.HolidaysScreen
import dev.danielgilchrist.knockoff.ui.preferences.LinkActions
import dev.danielgilchrist.knockoff.ui.preferences.LinkScreen
import dev.danielgilchrist.knockoff.ui.preferences.PreferencesActions
import dev.danielgilchrist.knockoff.ui.preferences.PreferencesScreen
import java.time.ZonedDateTime

@Composable
fun KnockoffScreen(
    viewModel: MainViewModel,
    system: SystemActions,
    modifier: Modifier = Modifier,
) {
    val setup by viewModel.setup.collectAsStateWithLifecycle()
    val paired by viewModel.paired.collectAsStateWithLifecycle()
    val editor = viewModel.editor
    val settings by editor.settings.collectAsStateWithLifecycle()
    val editing by editor.editing.collectAsStateWithLifecycle()
    val choices by viewModel.choices.collectAsStateWithLifecycle()
    val newChats by viewModel.newChats.collectAsStateWithLifecycle()
    val saveStatus by editor.saveStatus.collectAsStateWithLifecycle()
    val preferencesStatus by viewModel.preferences.saveStatus.collectAsStateWithLifecycle()
    val calendars by viewModel.calendars.collectAsStateWithLifecycle()
    val holidays by viewModel.holidays.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf(Screen.Home) }

    LifecycleResumeEffect(Unit) {
        viewModel.refreshSetup()
        onPauseOrDispose {}
    }

    val shown = ShownScreen.of(screen, settings)

    LaunchedEffect(shown) { screen = shown.screen }

    val navigate: (Screen) -> Unit = { screen = it }
    val closed: @Composable () -> Unit = {
        ClosedScreen(
            shown = shown,
            state =
                ClosedState(
                    setup,
                    paired,
                    settings,
                    saveStatus,
                    preferencesStatus,
                    newChats,
                    choices.apps,
                    calendars,
                    holidays,
                ),
            home = homeActions(viewModel, system, navigate),
            onBackToHome = { screen = Screen.Home },
            link = LinkActions(system, onBack = { screen = Screen.Preferences }),
            preferences = preferencesActions(viewModel, system, navigate),
            holidays = holidaysActions(viewModel, system, navigate),
        )
    }

    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
        when (val ready = settings) {
            SettingsState.Loading, is SettingsState.Failed -> {
                closed()
            }

            is SettingsState.Ready -> {
                OpenScreen(
                    editing = editing,
                    ready = ready,
                    facts = RuleFacts(choices, saveStatus, holidays),
                    editor = editor,
                    closed = closed,
                )
            }
        }
    }
}

@Composable
private fun OpenScreen(
    editing: Editing,
    ready: SettingsState.Ready,
    facts: RuleFacts,
    editor: SettingsEditor,
    closed: @Composable () -> Unit,
) {
    val choices = facts.choices

    when (editing) {
        Editing.Closed -> {
            closed()
        }

        is Editing.Viewing -> {
            RuleOverview(editing.rule, ready, facts, editor)
        }

        is Editing.Renaming -> {
            RuleOverview(editing.rule, ready, facts, editor) {
                RenameDialog(renaming = editing, actions = sectionActions(editor, editing.rule, choices.groups))
            }
        }

        is Editing.Section -> {
            SectionEditor(editing = editing, ready = ready, facts = facts, editor = editor)
        }

        is Editing.Guided -> {
            Wizard(guided = editing, choices = choices, editor = editor)
        }
    }
}

@Composable
private fun SectionEditor(
    editing: Editing.Section,
    ready: SettingsState.Ready,
    facts: RuleFacts,
    editor: SettingsEditor,
    modifier: Modifier = Modifier,
) {
    val defaultZone = ready.settings.rules.defaultZone
    val rule = editing.rule
    val day = RuleDay.of(rule, ZonedDateTime.now(), defaultZone, facts.holidays.holidays)

    RuleSectionScreen(
        editing = editing,
        choices = facts.choices,
        facts = SectionFacts(defaultZone, day.date, rule.policy, facts.holidays.describeSource()),
        actions = sectionActions(editor, editing.rule, facts.choices.groups),
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
                onCreate = { editor.saveDraft(choices.groups) },
                onCancel = { editor.show(Editing.Closed) },
                onFocus = editor::focus,
            ),
        modifier = modifier,
    )
}

private fun preferencesActions(
    viewModel: MainViewModel,
    system: SystemActions,
    onScreen: (Screen) -> Unit,
): PreferencesActions =
    PreferencesActions(
        system = system,
        onDefaultZoneChange = viewModel.preferences::setDefaultZone,
        onThemeChange = viewModel.preferences::setTheme,
        onOpenLink = { onScreen(Screen.Link) },
        onOpenHolidays = {
            viewModel.refreshCalendars()
            onScreen(Screen.Holidays)
        },
        onBack = { onScreen(Screen.Home) },
    )

private fun holidaysActions(
    viewModel: MainViewModel,
    system: SystemActions,
    onScreen: (Screen) -> Unit,
): HolidaysActions =
    HolidaysActions(
        onAllowCalendar = system.onAllowCalendar,
        onOpenAppSettings = system.onOpenAppSettings,
        onChoose = viewModel.preferences::setHolidays,
        onBack = { onScreen(Screen.Preferences) },
    )

private fun homeActions(
    viewModel: MainViewModel,
    system: SystemActions,
    onScreen: (Screen) -> Unit,
): HomeActions =
    HomeActions(
        system = system,
        onOpenPreferences = { onScreen(Screen.Preferences) },
        onOpenNewChats = { onScreen(Screen.NewChats) },
        onNewRule = { viewModel.editor.show(Editing.Guided.START) },
        onEditRule = { rule -> viewModel.editor.show(Editing.Viewing(rule)) },
        onRuleChange = viewModel.editor::changeRule,
        onLetChatThrough = viewModel::letChatThrough,
        onResetToDefaults = viewModel.editor::resetToDefaults,
    )

@Composable
private fun RuleOverview(
    shown: Rule,
    ready: SettingsState.Ready,
    facts: RuleFacts,
    editor: SettingsEditor,
    modifier: Modifier = Modifier,
    overlay: @Composable () -> Unit = {},
) {
    val rules = ready.settings.rules
    val rule = rules.rules.firstOrNull { it.name == shown.name } ?: shown
    val day = RuleDay.of(rule, ZonedDateTime.now(), rules.defaultZone, facts.holidays.holidays)

    RuleOverviewScreen(
        rule = rule,
        day = day,
        apps = facts.choices.apps,
        saveStatus = facts.saveStatus,
        actions =
            RuleOverviewActions(
                onBack = { editor.show(Editing.Closed) },
                onStateChange = { state -> editor.changeRule(rule.name, RuleChange.SetState(state)) },
                dayOff =
                    DayOffActions(
                        onTakeTodayOff = { editor.changeRule(rule.name, RuleChange.TakeOff(day.date)) },
                        onEndDayOff = { editor.changeRule(rule.name, RuleChange.BackOn(day.date)) },
                        onEditDaysOff = { editor.show(Editing.Section.of(rule, RuleSection.DaysOff)) },
                    ),
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
    link: LinkActions,
    preferences: PreferencesActions,
    holidays: HolidaysActions,
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
                holidays = state.holidays.holidays,
                actions = home,
                modifier = modifier,
            )
        }

        is ShownScreen.Preferences -> {
            PreferencesScreen(
                settings = shown.settings,
                holidays = state.holidays,
                setup = state.setup,
                saveStatus = state.preferencesStatus,
                actions = preferences,
                modifier = modifier,
            )
        }

        is ShownScreen.Holidays -> {
            HolidaysScreen(
                chosen = shown.settings.holidayCalendar,
                calendars = state.calendars,
                holidays = state.holidays,
                saveStatus = state.preferencesStatus,
                actions = holidays,
                modifier = modifier,
            )
        }

        ShownScreen.Link -> {
            LinkScreen(link = state.setup.link, paired = state.paired, actions = link, modifier = modifier)
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
    groups: KnownGroups,
): RuleSectionActions =
    RuleSectionActions(
        onDraftChange = editor::updateDraft,
        onSave = { editor.saveDraft(groups) },
        onCancel = { editor.show(Editing.Viewing(rule)) },
        onFocus = editor::focus,
    )
