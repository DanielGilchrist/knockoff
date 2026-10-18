package dev.danielgilchrist.knockoff.ui

import dev.danielgilchrist.knockoff.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.rule.Policy
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
    fun `guiding does nothing outside the wizard`() =
        runTest {
            val editor = editor()
            editor.guide { it.toStep(WizardStep.Review) }
            assertEquals(Editing.Closed, editor.editing.value)

            editor.show(Editing.Section.of(work, RuleSection.When))
            editor.guide { it.toStep(WizardStep.Review) }
            assertEquals(
                Editing.Section.of(work, RuleSection.When),
                editor.editing.value,
            )
        }

    @Test
    fun `problems creating from the wizard keep it on its step`() =
        runTest {
            val editor = editor()
            editor.show(Editing.Guided.START)
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
            editor.show(Editing.Guided.START)
            editor.guide { it.choosing(WizardGoal.WorkHours).toStep(WizardStep.Review) }

            editor.saveDraft()

            val guided = editor.editing.first { it is Editing.Guided && it.problems.isNotEmpty() }
            assertEquals(
                listOf(DraftProblem.DuplicateName),
                assertInstanceOf(Editing.Guided::class.java, guided).stepProblems(),
            )
        }

    @Test
    fun `editing a section of a rule starts from that rule`() =
        runTest {
            val editor = editor()

            editor.show(Editing.Section.of(work, RuleSection.What))

            assertEquals(
                Editing.Section(work, RuleSection.What, RuleDraft.from(work), emptyList()),
                editor.editing.value,
            )
        }

    @Test
    fun `saving a section shows the rule under its new name`() =
        runTest {
            val editor = editor()
            editor.readySettings()
            editor.show(Editing.Section.of(work, RuleSection.Name))
            editor.updateDraft(RuleDraft.from(work).copy(name = "Office"))

            editor.saveDraft()

            val office = assertValid(RuleDraft.from(work).copy(name = "Office"))
            assertEquals(Editing.Viewing(work.copy(name = office.name)), editor.editing.first { it is Editing.Viewing })
        }

    @Test
    fun `saving a valid new rule stores it and shows it`() =
        runTest {
            val editor = editor()
            editor.readySettings()

            editor.show(Editing.Guided.START)
            editor.updateDraft(RuleDraft.NEW.copy(name = "Evenings"))
            editor.saveDraft()

            editor.editing.first { it == Editing.Viewing(assertValid(RuleDraft.NEW.copy(name = "Evenings"))) }
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

            editor.show(Editing.Guided.START)
            editor.saveDraft()

            assertEquals(listOf(DraftProblem.BlankName), openEditing(editor).problems)
        }

    @Test
    fun `changing the draft clears earlier problems`() =
        runTest {
            val editor = editor()
            editor.show(Editing.Guided.START)
            editor.saveDraft()

            editor.updateDraft(RuleDraft.NEW.copy(name = "x"))

            assertEquals(emptyList<DraftProblem>(), openEditing(editor).problems)
        }

    @Test
    fun `a duplicate name keeps the editor open with the problem`() =
        runTest {
            val editor = editor()
            editor.readySettings()

            editor.show(Editing.Guided.START)
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

            editor.show(Editing.Guided.START)
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
            editor.show(Editing.Guided.START)
            editor.updateDraft(RuleDraft.NEW.copy(name = "Evenings"))
            editor.saveDraft()
            editor.editing.first { it is Editing.Viewing }

            editor.changeRule(work.name, RuleChange.SetState(RuleState.Disabled))
            val evenings = assertValid(RuleDraft.NEW.copy(name = "Evenings"))
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
    fun `saving a section keeps changes made elsewhere to the rest of the rule`() =
        runTest {
            val editor = editor()
            editor.readySettings()
            editor.show(Editing.Section.of(work, RuleSection.When))
            editor.updateDraft(RuleDraft.from(work).copy(policy = Policy.HoldDuring))
            editor.changeRule(work.name, RuleChange.AddChat(COLLEAGUE_CHAT))
            editor.settings.first {
                it is SettingsState.Ready &&
                    it.settings.rules.rules
                        .single()
                        .chats
                        .isNotEmpty()
            }

            editor.saveDraft()

            editor.settings.first {
                it is SettingsState.Ready &&
                    it.settings.rules.rules
                        .single() == work.withChat(COLLEAGUE_CHAT).copy(policy = Policy.HoldDuring)
            }
        }

    @Test
    fun `a failed delete keeps the rule shown and says why`() =
        runTest {
            val editor = editor()
            editor.readySettings()
            editor.show(Editing.Viewing(work))
            directory.setWritable(false)

            editor.deleteRule(work.name)

            editor.saveStatus.first { it is SaveStatus.Failed }
            assertEquals(Editing.Viewing(work), editor.editing.value)
            directory.setWritable(true)
        }

    @Test
    fun `deleting the shown rule closes it`() =
        runTest {
            val editor = editor()
            editor.readySettings()
            editor.show(Editing.Viewing(work))

            editor.deleteRule(work.name)

            editor.editing.first { it == Editing.Closed }
            editor.settings.first {
                it is SettingsState.Ready &&
                    it.settings.rules.rules
                        .isEmpty()
            }
        }

    @Test
    fun `a save finishing after the editor moved on leaves the new editor open`() =
        runTest {
            val editor = editor()
            editor.readySettings()
            editor.show(Editing.Guided.START)
            editor.updateDraft(RuleDraft.NEW.copy(name = "Evenings"))

            editor.saveDraft()
            editor.show(Editing.Section.of(work, RuleSection.When))
            editor.settings.first { it is SettingsState.Ready && it.settings.rules.rules.size == 2 }

            assertEquals(
                Editing.Section.of(work, RuleSection.When),
                editor.editing.value,
            )
        }

    @Test
    fun `a save failing after the editor moved on leaves the new editor's problems alone`() =
        runTest {
            val editor = editor()
            editor.readySettings()
            editor.show(Editing.Guided.START)
            editor.updateDraft(RuleDraft.NEW.copy(name = "Work"))

            editor.saveDraft()
            editor.show(Editing.Section.of(work, RuleSection.When))
            editor.changeRule(work.name, RuleChange.SetState(RuleState.Disabled))
            editor.settings.first {
                it is SettingsState.Ready &&
                    it.settings.rules.rules
                        .single()
                        .state == RuleState.Disabled
            }
            testScheduler.advanceUntilIdle()

            assertEquals(
                Editing.Section.of(work, RuleSection.When),
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
