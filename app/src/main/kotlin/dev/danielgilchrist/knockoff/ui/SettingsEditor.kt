package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.rule.RuleName
import dev.danielgilchrist.knockoff.domain.settings.Settings
import dev.danielgilchrist.knockoff.settings.SettingsLoad
import dev.danielgilchrist.knockoff.settings.SettingsSave
import dev.danielgilchrist.knockoff.settings.SettingsStore
import dev.danielgilchrist.knockoff.settings.SettingsUpdate
import dev.danielgilchrist.knockoff.ui.editor.Checked
import dev.danielgilchrist.knockoff.ui.editor.DraftProblem
import dev.danielgilchrist.knockoff.ui.editor.GoalChoice
import dev.danielgilchrist.knockoff.ui.editor.RuleDraft
import dev.danielgilchrist.knockoff.ui.editor.WizardStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsEditor(
    private val store: SettingsStore,
    private val scope: CoroutineScope,
) {
    private val mutableEditing = MutableStateFlow<Editing>(Editing.Closed)
    private val mutableSaveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)

    val editing: StateFlow<Editing> = mutableEditing.asStateFlow()
    val saveStatus: StateFlow<SaveStatus> = mutableSaveStatus.asStateFlow()
    val settings: StateFlow<SettingsState> =
        store.loads
            .map { load ->
                when (load) {
                    is SettingsLoad.Loaded -> SettingsState.Ready(load.settings, SettingsOrigin.Saved)
                    is SettingsLoad.Empty -> SettingsState.Ready(load.settings, SettingsOrigin.Defaults)
                    is SettingsLoad.Failure -> SettingsState.Failed(load.detail)
                }
            }.stateIn(scope, SharingStarted.Eagerly, SettingsState.Loading)

    fun startEditing(target: EditTarget) {
        mutableEditing.value =
            when (target) {
                EditTarget.New -> Editing.Guided(RuleDraft.NEW, emptyList(), WizardStep.Goal, GoalChoice.Unchosen)
                is EditTarget.Existing -> Editing.Open(target, RuleDraft.from(target.rule), emptyList())
            }
    }

    fun guide(change: (Editing.Guided) -> Editing.Active) {
        mutableEditing.update { editing ->
            when (editing) {
                Editing.Closed, is Editing.Open -> editing
                is Editing.Guided -> change(editing)
            }
        }
    }

    fun updateDraft(draft: RuleDraft) {
        mutableEditing.update { editing ->
            when (editing) {
                Editing.Closed -> editing
                is Editing.Active -> editing.with(draft, emptyList())
            }
        }
    }

    fun closeEditor() {
        mutableEditing.value = Editing.Closed
    }

    fun saveDraft() {
        when (val editing = mutableEditing.value) {
            Editing.Closed -> {}

            is Editing.Active -> {
                when (val checked = editing.draft.parse()) {
                    is Checked.Problems -> {
                        mutableEditing.value = editing.with(editing.draft, checked.problems)
                    }

                    is Checked.Ok -> {
                        scope.launch {
                            closeOrShowProblems(editing, store.update { it.withRule(editing.target, checked.value) })
                        }
                    }
                }
            }
        }
    }

    fun deleteRule(name: RuleName) {
        when (val editing = mutableEditing.value) {
            Editing.Closed -> {}

            is Editing.Active -> {
                scope.launch { closeOrShowProblems(editing, store.update { it.withoutRule(name) }) }
            }
        }
    }

    fun changeRule(
        name: RuleName,
        change: RuleChange,
    ) {
        scope.launch { report(store.update { it.withRuleChanged(name, change) }) }
    }

    fun resetToDefaults() {
        scope.launch {
            mutableSaveStatus.value =
                when (val save = store.save(Settings.DEFAULT)) {
                    SettingsSave.Saved -> SaveStatus.Idle
                    is SettingsSave.WriteFailed -> SaveStatus.Failed(save.detail)
                }
        }
    }

    private fun closeOrShowProblems(
        saving: Editing.Active,
        update: SettingsUpdate,
    ) {
        mutableEditing.update { editing ->
            when (editing) {
                Editing.Closed -> {
                    editing
                }

                is Editing.Active -> {
                    when {
                        editing.target != saving.target -> editing
                        else -> editing.afterSaving(update)
                    }
                }
            }
        }
    }

    private fun Editing.Active.afterSaving(update: SettingsUpdate): Editing =
        when (update) {
            SettingsUpdate.Saved -> Editing.Closed
            is SettingsUpdate.DuplicateNames -> with(draft, listOf(DraftProblem.DuplicateName))
            is SettingsUpdate.Failure -> with(draft, listOf(DraftProblem.NotSaved(update.detail)))
        }

    private fun report(update: SettingsUpdate) {
        mutableSaveStatus.value =
            when (update) {
                SettingsUpdate.Saved -> {
                    SaveStatus.Idle
                }

                is SettingsUpdate.DuplicateNames -> {
                    SaveStatus.Failed(update.detail)
                }

                is SettingsUpdate.Failure -> {
                    SaveStatus.Failed(update.detail)
                }
            }
    }
}
