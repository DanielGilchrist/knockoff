package dev.danielgilchrist.knockoff.listener

import dev.danielgilchrist.knockoff.domain.change.Baseline
import dev.danielgilchrist.knockoff.domain.chat.SeenChats
import dev.danielgilchrist.knockoff.domain.rule.Evaluation
import dev.danielgilchrist.knockoff.domain.rule.Policy
import dev.danielgilchrist.knockoff.domain.rule.RuleSetParse
import dev.danielgilchrist.knockoff.domain.settings.AppliedRules
import dev.danielgilchrist.knockoff.domain.settings.Settings
import dev.danielgilchrist.knockoff.settings.AppliedRulesKeeper
import dev.danielgilchrist.knockoff.settings.AppliedRulesLoad
import dev.danielgilchrist.knockoff.settings.AppliedRulesSave
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.ZoneId

class RulesApplierTest {
    private val zone = ZoneId.of("Europe/London")
    private val s = AppliedRules(Settings.DEFAULT.rules, zone)
    private val a = s.withPolicy(Policy.HoldDuring)
    private val b = AppliedRules(Settings.DEFAULT.rules, ZoneId.of("Europe/Paris"))

    @Test
    fun `with nothing applied before, the first rules are swept and saved without a recheck`() =
        runTest {
            val held = FakeHeld()
            val keeper = FakeKeeper(AppliedRulesLoad.Missing)
            val applier = RulesApplier(held, {}, keeper) {}

            applier.apply(applying(s))

            assertEquals(listOf(s.evaluation(SeenChats.NONE)), held.swept)
            assertEquals(emptyList<Pair<Baseline.Known<Evaluation>, Evaluation>>(), held.rechecked)
            assertEquals(listOf(Baseline.Known.of(s)), keeper.saved)
            assertEquals(Applied.Active(s.evaluation(SeenChats.NONE)), applier.applied)
        }

    @Test
    fun `rules unchanged since last time are swept without a recheck or a save`() =
        runTest {
            val held = FakeHeld()
            val keeper = FakeKeeper(AppliedRulesLoad.Loaded(Baseline.Known.of(s)))

            RulesApplier(held, {}, keeper) {}.apply(applying(s))

            assertEquals(emptyList<Pair<Baseline.Known<Evaluation>, Evaluation>>(), held.rechecked)
            assertEquals(emptyList<Baseline.Known<AppliedRules>>(), keeper.saved)
        }

    @Test
    fun `rules changed since last time are rechecked and saved only once the recheck finishes`() =
        runTest {
            val job = Job()
            val held = FakeHeld(RecheckStart.Started(job))
            val keeper = FakeKeeper(AppliedRulesLoad.Loaded(Baseline.Known.of(s)))
            val applier = RulesApplier(held, {}, keeper) {}

            val applying = launch(start = CoroutineStart.UNDISPATCHED) { applier.apply(applying(a)) }

            assertEquals(listOf(Baseline.Known.of(ev(s)) to ev(a)), held.rechecked)
            assertEquals(emptyList<Baseline.Known<AppliedRules>>(), keeper.saved)

            job.complete()
            applying.join()

            assertEquals(listOf(Baseline.Known.of(a)), keeper.saved)
        }

    @Test
    fun `a skipped recheck saves the widened baseline and the next change rechecks from all of it`() =
        runTest {
            val held = FakeHeld(RecheckStart.Skipped("unavailable"), RecheckStart.Started(completed()))
            val keeper = FakeKeeper(AppliedRulesLoad.Loaded(Baseline.Known.of(s)))
            val applier = RulesApplier(held, {}, keeper) {}

            applier.apply(applying(a))
            assertEquals(listOf(Baseline.Known(s, listOf(a))), keeper.saved)

            applier.apply(applying(b))

            assertEquals(
                listOf(
                    Baseline.Known.of(ev(s)) to ev(a),
                    Baseline.Known(ev(s), listOf(ev(a))) to ev(b),
                ),
                held.rechecked,
            )
            assertEquals(listOf(Baseline.Known(s, listOf(a)), Baseline.Known.of(b)), keeper.saved)
        }

    @Test
    fun `the last applied rules are read once, on the first rules`() =
        runTest {
            val keeper = FakeKeeper(AppliedRulesLoad.Loaded(Baseline.Known.of(s)))
            val applier =
                RulesApplier(
                    FakeHeld(RecheckStart.Started(completed()), RecheckStart.Started(completed())),
                    {},
                    keeper,
                ) {}

            applier.apply(applying(a))
            applier.apply(applying(b))

            assertEquals(1, keeper.loads)
        }

    @Test
    fun `startup says how it compared and silencing is asked for each time`() =
        runTest {
            val reports = mutableListOf<String>()
            var silencing = 0
            val applier =
                RulesApplier(FakeHeld(), { silencing += 1 }, FakeKeeper(AppliedRulesLoad.Failed("broken"))) {
                    reports += it
                }

            applier.apply(applying(s))

            assertEquals(listOf("last applied rules unreadable, not rechecking: broken"), reports)
            assertEquals(1, silencing)
        }

    @Test
    fun `going back to the reconciled rules after a skip still rechecks`() =
        runTest {
            val held = FakeHeld(RecheckStart.Skipped("unavailable"), RecheckStart.Started(completed()))
            val keeper = FakeKeeper(AppliedRulesLoad.Loaded(Baseline.Known.of(s)))
            val applier = RulesApplier(held, {}, keeper) {}

            applier.apply(applying(a))
            applier.apply(applying(s))

            assertEquals(Baseline.Known(ev(s), listOf(ev(a))) to ev(s), held.rechecked.last())
            assertEquals(listOf(Baseline.Known(s, listOf(a)), Baseline.Known.of(s)), keeper.saved)
        }

    @Test
    fun `a widened baseline from the last run is rechecked from all of it`() =
        runTest {
            val held = FakeHeld(RecheckStart.Started(completed()))
            val keeper = FakeKeeper(AppliedRulesLoad.Loaded(Baseline.Known(s, listOf(a))))

            RulesApplier(held, {}, keeper) {}.apply(applying(s))

            assertEquals(listOf(Baseline.Known(ev(s), listOf(ev(a))) to ev(s)), held.rechecked)
            assertEquals(listOf(Baseline.Known.of(s)), keeper.saved)
        }

    private fun applying(rules: AppliedRules) = Applying(rules, SeenChats.NONE)

    private fun ev(rules: AppliedRules): Evaluation = rules.evaluation(SeenChats.NONE)

    private fun completed(): CompletableJob = Job().also { it.complete() }

    private fun AppliedRules.withPolicy(policy: Policy): AppliedRules {
        val rule = rules.rules.single().copy(policy = policy)

        return when (val parse = rules.withRules(listOf(rule))) {
            is RuleSetParse.Valid -> AppliedRules(parse.rules, zone)
            is RuleSetParse.DuplicateNames -> error("one rule can't duplicate a name")
        }
    }

    private class FakeHeld(
        vararg starts: RecheckStart,
    ) : HeldNotifications {
        private val queued = starts.toMutableList()
        val swept = mutableListOf<Evaluation>()
        val rechecked = mutableListOf<Pair<Baseline.Known<Evaluation>, Evaluation>>()

        override fun sweep(evaluation: Evaluation) {
            swept += evaluation
        }

        override fun recheckSnoozed(
            before: Baseline.Known<Evaluation>,
            after: Evaluation,
        ): RecheckStart {
            rechecked += before to after
            return queued.removeAt(0)
        }
    }

    private class FakeKeeper(
        private val load: AppliedRulesLoad,
    ) : AppliedRulesKeeper {
        var loads = 0
        val saved = mutableListOf<Baseline.Known<AppliedRules>>()

        override suspend fun load(): AppliedRulesLoad {
            loads += 1
            return load
        }

        override suspend fun save(baseline: Baseline.Known<AppliedRules>): AppliedRulesSave {
            saved += baseline
            return AppliedRulesSave.Saved
        }
    }
}
