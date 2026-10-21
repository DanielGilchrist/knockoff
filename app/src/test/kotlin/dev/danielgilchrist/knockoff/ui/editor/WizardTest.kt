package dev.danielgilchrist.knockoff.ui.editor

import dev.danielgilchrist.knockoff.COLLEAGUE_CHAT
import dev.danielgilchrist.knockoff.domain.notification.PackageName
import dev.danielgilchrist.knockoff.domain.rule.NewChatsAsking
import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.ui.AppFocus
import dev.danielgilchrist.knockoff.ui.Editing
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.DayOfWeek
import java.time.LocalTime

private val SLACK_APP = PackageName("com.Slack")

class WizardTest {
    companion object {
        @JvmStatic
        fun problemSteps(): List<Arguments> =
            listOf(
                Arguments.of(DraftProblem.BlankName, WizardStep.Review),
                Arguments.of(DraftProblem.DuplicateName, WizardStep.Review),
                Arguments.of(DraftProblem.NotSaved("x"), WizardStep.Review),
                Arguments.of(DraftProblem.NoHours, WizardStep.Hours),
                Arguments.of(DraftProblem.BreaksCoverAllHours, WizardStep.Hours),
                Arguments.of(DraftProblem.HoursWithoutDays(1), WizardStep.Hours),
                Arguments.of(DraftProblem.BlankBreakName(1), WizardStep.Hours),
                Arguments.of(DraftProblem.BreakWithoutDays(1), WizardStep.Hours),
                Arguments.of(DraftProblem.NothingChosen(SLACK_APP), WizardStep.Apps),
                Arguments.of(DraftProblem.BlankPattern(SLACK_APP, 1), WizardStep.Apps),
                Arguments.of(DraftProblem.InvalidPattern(SLACK_APP, 1, "x"), WizardStep.Apps),
            )
    }

    private val start = Editing.Guided.START
    private val quietHours = WindowDraft(DayOfWeek.entries.toSet(), LocalTime.of(21, 0), LocalTime.of(7, 0))
    private val dayJob = WindowDraft(setOf(DayOfWeek.MONDAY), LocalTime.of(9, 0), LocalTime.of(15, 0))

    @Test
    fun `steps are numbered from one`() {
        assertEquals(listOf(1, 2, 3, 4), WizardStep.entries.map { it.position })
    }

    @Test
    fun `steps stop at either end`() {
        assertEquals(WizardStep.Goal, WizardStep.Goal.previous())
        assertEquals(WizardStep.Review, WizardStep.Review.next())
    }

    @ParameterizedTest
    @MethodSource("problemSteps")
    fun `every problem belongs to a step`(
        problem: DraftProblem,
        step: WizardStep,
    ) {
        assertEquals(step, WizardStep.of(problem))
    }

    @Test
    fun `an app narrowed to nothing blocks the step`() {
        val apps = start.choosing(WizardGoal.WorkHours).toStep(WizardStep.Apps)
        val nothing = AppDraft.EVERYTHING.copy(mode = AppMode.Narrowed)
        val invalid = apps.copy(draft = apps.draft.copy(apps = mapOf(SLACK_APP to nothing)))

        assertEquals(false, invalid.canContinue())
    }

    @Test
    fun `only work hours offer a lunch break`() {
        assertEquals(listOf(true, false), WizardGoal.entries.map { it.offersLunch })
    }

    @Test
    fun `asking about an app's new chats counts as a pick`() {
        val work = start.choosing(WizardGoal.WorkHours).toStep(WizardStep.Apps)
        val asking = AppDraft.EVERYTHING.copy(mode = AppMode.Narrowed, asking = NewChatsAsking.Ask)

        assertEquals(
            true,
            work.copy(draft = work.draft.copy(apps = mapOf(COLLEAGUE_CHAT.packageName to asking))).canContinue(),
        )
    }

    @Test
    fun `opening an app focuses it and moving step returns to the list`() {
        val apps = start.choosing(WizardGoal.WorkHours).toStep(WizardStep.Apps)
        val focused = apps.focusing(AppFocus.App(SLACK_APP))

        assertEquals(AppFocus.App(SLACK_APP), focused.focus)
        assertEquals(AppFocus.List, focused.toStep(WizardStep.Hours).focus)
    }

    @Test
    fun `steps run in order`() {
        assertEquals(
            listOf(WizardStep.Apps, WizardStep.Hours, WizardStep.Review),
            listOf(WizardStep.Goal, WizardStep.Apps, WizardStep.Hours).map { it.next() },
        )
        assertEquals(
            listOf(WizardStep.Goal, WizardStep.Apps, WizardStep.Hours),
            listOf(WizardStep.Apps, WizardStep.Hours, WizardStep.Review).map { it.previous() },
        )
    }

    @Test
    fun `choosing a goal sets its defaults`() {
        val work = start.choosing(WizardGoal.WorkHours)
        val quiet = start.choosing(WizardGoal.QuietTime)

        assertEquals(GoalChoice.Chosen(WizardGoal.WorkHours), work.goal)
        assertEquals(RuleDraft.NEW.copy(name = "Work"), work.draft)
        assertEquals(
            RuleDraft.NEW.copy(name = "Quiet time", policy = Policy.HoldDuring, windows = listOf(quietHours)),
            quiet.draft,
        )
    }

    @Test
    fun `choosing the same goal again changes nothing`() {
        val edited = start.choosing(WizardGoal.WorkHours).withHours(dayJob)

        assertEquals(edited, edited.choosing(WizardGoal.WorkHours))
    }

    @Test
    fun `switching goal keeps a typed name and changed hours`() {
        val edited = start.choosing(WizardGoal.WorkHours).withHours(dayJob)
        val named = edited.copy(draft = edited.draft.copy(name = "Day job"))

        val switched = named.choosing(WizardGoal.QuietTime)

        assertEquals("Day job", switched.draft.name)
        assertEquals(listOf(dayJob), switched.draft.windows)
        assertEquals(emptyList<BreakDraft>(), switched.draft.breaks)
        assertEquals(Policy.HoldDuring, switched.draft.policy)
    }

    @Test
    fun `switching goal replaces untouched defaults and drops the lunch break with them`() {
        val work = start.choosing(WizardGoal.WorkHours)
        val withLunch = work.copy(draft = work.draft.withBreakAdded())

        val switched = withLunch.choosing(WizardGoal.QuietTime)

        assertEquals("Quiet time", switched.draft.name)
        assertEquals(listOf(quietHours), switched.draft.windows)
        assertEquals(emptyList<BreakDraft>(), switched.draft.breaks)
    }

    @Test
    fun `switching to quiet time drops a lunch break even after the hours changed`() {
        val work = start.choosing(WizardGoal.WorkHours)
        val edited = work.copy(draft = work.draft.withBreakAdded()).withHours(dayJob)

        assertEquals(emptyList<BreakDraft>(), edited.choosing(WizardGoal.QuietTime).draft.breaks)
    }

    @Test
    fun `choosing a goal keeps what was picked`() {
        val picked =
            start.copy(
                draft =
                    RuleDraft.NEW.copy(
                        apps = mapOf(SLACK_APP to AppDraft.EVERYTHING),
                    ),
            )

        val chosen = picked.choosing(WizardGoal.QuietTime).draft

        assertEquals(picked.draft.apps, chosen.apps)
        assertEquals(picked.draft.state, chosen.state)
        assertEquals(picked.draft.zone, chosen.zone)
    }

    @Test
    fun `a lunch break follows the days of the hours`() {
        val work = start.choosing(WizardGoal.WorkHours)
        val withLunch = work.copy(draft = work.draft.withBreakAdded())

        val changed = withLunch.withHours(dayJob)

        assertEquals(
            setOf(DayOfWeek.MONDAY),
            changed.draft.breaks
                .single()
                .window.days,
        )
    }

    @Test
    fun `each step needs its own choices before continuing`() {
        val work = start.choosing(WizardGoal.WorkHours)
        val picked =
            work.copy(
                step = WizardStep.Apps,
                draft =
                    work.draft.copy(
                        apps =
                            mapOf(SLACK_APP to AppDraft.EVERYTHING),
                    ),
            )
        val noDays = picked.copy(step = WizardStep.Hours).withHours(dayJob.copy(days = emptySet()))

        assertEquals(false, start.canContinue())
        assertEquals(true, work.canContinue())
        assertEquals(false, work.copy(step = WizardStep.Apps).canContinue())
        assertEquals(true, picked.canContinue())
        assertEquals(false, noDays.canContinue())
        assertEquals(listOf(DraftProblem.HoursWithoutDays(1)), noDays.stepProblems())
    }

    @Test
    fun `problems show on the step they belong to`() {
        val unnamed =
            start.copy(
                step = WizardStep.Review,
                draft = RuleDraft.NEW.copy(windows = listOf(dayJob.copy(days = emptySet()))),
            )

        assertEquals(listOf(DraftProblem.BlankName), unnamed.stepProblems())
        assertEquals(listOf(DraftProblem.HoursWithoutDays(1)), unnamed.toStep(WizardStep.Hours).stepProblems())
    }
}
