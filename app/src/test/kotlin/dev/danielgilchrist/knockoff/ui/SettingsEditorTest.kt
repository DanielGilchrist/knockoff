package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.domain.rule.Rule
import dev.danielgilchrist.knockoff.domain.rule.RuleState
import dev.danielgilchrist.knockoff.domain.settings.Settings
import dev.danielgilchrist.knockoff.settings.SettingsStore
import dev.danielgilchrist.knockoff.ui.editor.Checked
import dev.danielgilchrist.knockoff.ui.editor.DraftProblem
import dev.danielgilchrist.knockoff.ui.editor.GoalChoice
import dev.danielgilchrist.knockoff.ui.editor.RuleDraft
import dev.danielgilchrist.knockoff.ui.editor.WizardGoal
import dev.danielgilchrist.knockoff.ui.editor.WizardStep
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class SettingsEditorTest {
    @TempDir
    lateinit var directory: File

    private val work: Rule =
        Settings.DEFAULT.rules.rules
            .single()

    @Test
    fun `a missing file shows the defaults as unsaved`() =
        runTest {
            assertEquals(SettingsState.Ready(Settings.DEFAULT, SettingsOrigin.Defaults), editor().readySettings())
        }

    @Test
    fun `a new rule starts in the wizard from the new draft`() =
        runTest {
            val editor = editor()

            editor.startEditing(EditTarget.New)

            assertEquals(
                Editing.Guided(RuleDraft.NEW, emptyList(), WizardStep.Goal, GoalChoice.Unchosen),
                editor.editing.value,
            )
        }

    @Test
    fun `the wizard hands over to the full editor keeping the draft`() =
        runTest {
            val editor = editor()
            editor.startEditing(EditTarget.New)
            editor.guide { it.toStep(WizardStep.When) }
            editor.updateDraft(RuleDraft.NEW.copy(name = "Evenings"))

            editor.guide { it.inFullEditor() }

            assertEquals(
                Editing.Open(EditTarget.New, RuleDraft.NEW.copy(name = "Evenings"), emptyList()),
                editor.editing.value,
            )
        }

    @Test
    fun `guiding does nothing outside the wizard`() =
        runTest {
            val editor = editor()
            editor.guide { it.toStep(WizardStep.Review) }
            assertEquals(Editing.Closed, editor.editing.value)

            editor.startEditing(EditTarget.Existing(work))
            editor.guide { it.toStep(WizardStep.Review) }
            assertEquals(
                Editing.Open(EditTarget.Existing(work), RuleDraft.from(work), emptyList()),
                editor.editing.value,
            )
        }

    @Test
    fun `problems creating from the wizard keep it on its step`() =
        runTest {
            val editor = editor()
            editor.startEditing(EditTarget.New)
            editor.guide { it.toStep(WizardStep.Review) }

            editor.saveDraft()

            val guided = assertInstanceOf(Editing.Guided::class.java, editor.editing.value)
            assertEquals(WizardStep.Review, guided.step)
            assertEquals(listOf(DraftProblem.BlankName), guided.stepProblems())
        }

    @Test
    fun `a duplicate name from the wizard is shown once on its review step`() =
        runTest {
            val editor = editor()
            editor.settings.first { it is SettingsState.Ready }
            editor.startEditing(EditTarget.New)
            editor.guide { it.choosing(WizardGoal.WorkHours).toStep(WizardStep.Review) }

            editor.saveDraft()

            val guided = editor.editing.first { it is Editing.Guided && it.problems.isNotEmpty() }
            assertEquals(
                listOf(DraftProblem.DuplicateName),
                assertInstanceOf(Editing.Guided::class.java, guided).stepProblems(),
            )
        }

    @Test
    fun `editing an existing rule starts from that rule`() =
        runTest {
            val editor = editor()

            editor.startEditing(EditTarget.Existing(work))

            assertEquals(
                Editing.Open(EditTarget.Existing(work), RuleDraft.from(work), emptyList()),
                editor.editing.value,
            )
        }

    @Test
    fun `saving a valid new rule stores it and closes the editor`() =
        runTest {
            val editor = editor()
            editor.readySettings()

            editor.startEditing(EditTarget.New)
            editor.updateDraft(RuleDraft.NEW.copy(name = "Evenings"))
            editor.saveDraft()

            editor.editing.first { it == Editing.Closed }
            val saved = editor.settings.first { it is SettingsState.Ready && it.origin == SettingsOrigin.Saved }
            assertEquals(
                listOf("Work", "Evenings"),
                assertInstanceOf(SettingsState.Ready::class.java, saved)
                    .settings.rules.rules
                    .map { it.name.value },
            )
        }

    @Test
    fun `an invalid draft keeps the editor open with its problems`() =
        runTest {
            val editor = editor()

            editor.startEditing(EditTarget.New)
            editor.saveDraft()

            assertEquals(listOf(DraftProblem.BlankName), openEditing(editor).problems)
        }

    @Test
    fun `changing the draft clears earlier problems`() =
        runTest {
            val editor = editor()
            editor.startEditing(EditTarget.New)
            editor.saveDraft()

            editor.updateDraft(RuleDraft.NEW.copy(name = "x"))

            assertEquals(emptyList<DraftProblem>(), openEditing(editor).problems)
        }

    @Test
    fun `a duplicate name keeps the editor open with the problem`() =
        runTest {
            val editor = editor()
            editor.readySettings()

            editor.startEditing(EditTarget.New)
            editor.updateDraft(RuleDraft.NEW.copy(name = "Work"))
            editor.saveDraft()

            val problems = editor.editing.first { it is Editing.Active && it.problems.isNotEmpty() }
            assertEquals(
                listOf(DraftProblem.DuplicateName),
                assertInstanceOf(Editing.Active::class.java, problems).problems,
            )
        }

    @Test
    fun `saving over a corrupt file keeps the draft and says why`() =
        runTest {
            settingsFile().writeText("not json")
            val editor = editor()

            editor.startEditing(EditTarget.New)
            editor.updateDraft(RuleDraft.NEW.copy(name = "Evenings"))
            editor.saveDraft()

            val open =
                assertInstanceOf(
                    Editing.Active::class.java,
                    editor.editing.first {
                        it is Editing.Active &&
                            it.problems.isNotEmpty()
                    },
                )
            val problem = open.problems.single()
            assertInstanceOf(DraftProblem.NotSaved::class.java, problem)
            assertEquals("Evenings", open.draft.name)
        }

    @Test
    fun `replacing a corrupt file with defaults reloads the settings`() =
        runTest {
            settingsFile().writeText("not json")
            val editor = editor()
            editor.settings.first { it is SettingsState.Failed }

            editor.resetToDefaults()

            assertEquals(
                SettingsState.Ready(Settings.DEFAULT, SettingsOrigin.Saved),
                editor.settings.first { it is SettingsState.Ready },
            )
        }

    @Test
    fun `replacing an unreadable file with defaults recovers once it can be read`() =
        runTest {
            settingsFile().writeText(Settings.DEFAULT.toJson())
            settingsFile().setReadable(false)
            val editor = editor()
            editor.settings.first { it is SettingsState.Failed }
            settingsFile().setReadable(true)

            editor.resetToDefaults()

            assertEquals(
                SettingsState.Ready(Settings.DEFAULT, SettingsOrigin.Saved),
                editor.settings.first { it is SettingsState.Ready },
            )
        }

    @Test
    fun `toggling one rule then deleting another keeps both changes`() =
        runTest {
            val editor = editor()
            editor.readySettings()
            editor.startEditing(EditTarget.New)
            editor.updateDraft(RuleDraft.NEW.copy(name = "Evenings"))
            editor.saveDraft()
            editor.editing.first { it == Editing.Closed }

            editor.changeRule(work.name, RuleChange.SetState(RuleState.Disabled))
            val evenings = assertValid(RuleDraft.NEW.copy(name = "Evenings"))
            editor.startEditing(EditTarget.Existing(evenings))
            editor.deleteRule(evenings.name)

            val expected = listOf(work.copy(state = RuleState.Disabled))
            editor.settings.first { it is SettingsState.Ready && it.settings.rules.rules == expected }
        }

    @Test
    fun `a failed toggle is reported and a later success clears it`() =
        runTest {
            settingsFile().writeText(Settings.DEFAULT.toJson())
            settingsFile().setReadable(false)
            val editor = editor()

            editor.changeRule(work.name, RuleChange.SetState(RuleState.Disabled))

            editor.saveStatus.first { it is SaveStatus.Failed }

            settingsFile().setReadable(true)
            editor.changeRule(work.name, RuleChange.SetState(RuleState.Disabled))

            editor.saveStatus.first { it == SaveStatus.Idle }
        }

    @Test
    fun `a failed reset is reported`() =
        runTest {
            directory.setWritable(false)
            val editor = editor()

            editor.resetToDefaults()

            editor.saveStatus.first { it is SaveStatus.Failed }
            directory.setWritable(true)
        }

    @Test
    fun `a failed delete keeps the editor open and says why`() =
        runTest {
            val editor = editor()
            editor.readySettings()
            editor.startEditing(EditTarget.Existing(work))
            directory.setWritable(false)

            editor.deleteRule(work.name)

            val open =
                assertInstanceOf(
                    Editing.Active::class.java,
                    editor.editing.first {
                        it is Editing.Active &&
                            it.problems.isNotEmpty()
                    },
                )
            assertInstanceOf(DraftProblem.NotSaved::class.java, open.problems.single())
            assertEquals(RuleDraft.from(work), open.draft)
            directory.setWritable(true)
        }

    @Test
    fun `a save finishing after the editor moved on leaves the new editor open`() =
        runTest {
            val editor = editor()
            editor.readySettings()
            editor.startEditing(EditTarget.New)
            editor.updateDraft(RuleDraft.NEW.copy(name = "Evenings"))

            editor.saveDraft()
            editor.startEditing(EditTarget.Existing(work))
            editor.settings.first { it is SettingsState.Ready && it.settings.rules.rules.size == 2 }

            assertEquals(
                Editing.Open(EditTarget.Existing(work), RuleDraft.from(work), emptyList()),
                editor.editing.value,
            )
        }

    @Test
    fun `a save failing after the editor moved on leaves the new editor's problems alone`() =
        runTest {
            val editor = editor()
            editor.readySettings()
            editor.startEditing(EditTarget.New)
            editor.updateDraft(RuleDraft.NEW.copy(name = "Work"))

            editor.saveDraft()
            editor.startEditing(EditTarget.Existing(work))
            editor.changeRule(work.name, RuleChange.SetState(RuleState.Disabled))
            editor.settings.first {
                it is SettingsState.Ready &&
                    it.settings.rules.rules
                        .single()
                        .state == RuleState.Disabled
            }
            testScheduler.advanceUntilIdle()

            assertEquals(
                Editing.Open(EditTarget.Existing(work), RuleDraft.from(work), emptyList()),
                editor.editing.value,
            )
        }

    private fun TestScope.editor(): SettingsEditor = SettingsEditor(SettingsStore(settingsFile()), backgroundScope)

    private suspend fun SettingsEditor.readySettings(): SettingsState = settings.first { it is SettingsState.Ready }

    private fun openEditing(editor: SettingsEditor): Editing.Active =
        assertInstanceOf(Editing.Active::class.java, editor.editing.value)

    private fun assertValid(draft: RuleDraft): Rule =
        when (val checked = draft.parse()) {
            is Checked.Ok -> checked.value
            is Checked.Problems -> error("Unexpected problems ${checked.problems}")
        }

    private fun settingsFile(): File = File(directory, "settings.json")
}
