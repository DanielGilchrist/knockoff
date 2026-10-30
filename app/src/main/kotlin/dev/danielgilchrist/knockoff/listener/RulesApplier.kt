package dev.danielgilchrist.knockoff.listener

import dev.danielgilchrist.knockoff.domain.change.Baseline
import dev.danielgilchrist.knockoff.domain.change.Step
import dev.danielgilchrist.knockoff.domain.change.stepTo
import dev.danielgilchrist.knockoff.domain.chat.SeenChats
import dev.danielgilchrist.knockoff.domain.dayoff.Holidays
import dev.danielgilchrist.knockoff.domain.rule.Evaluation
import dev.danielgilchrist.knockoff.settings.AppliedRulesKeeper
import dev.danielgilchrist.knockoff.settings.AppliedRulesLoad
import dev.danielgilchrist.knockoff.settings.AppliedRulesSave
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal class RulesApplier(
    private val held: HeldNotifications,
    private val requestSilencing: () -> Unit,
    private val keeper: AppliedRulesKeeper,
    private val scope: CoroutineScope,
    private val report: (String) -> Unit,
) {
    companion object {
        private val RECHECK_RETRIES = listOf(30.seconds, 2.minutes, 10.minutes)
    }

    @Volatile
    var applied: Applied = Applied.Pending
        private set

    private var state: BaselineState = BaselineState.NotLoaded
    private val applyLock = Mutex()

    suspend fun apply(applying: Applying) {
        applyLock.withLock { applyNow(applying, attempt = 0) }
    }

    private suspend fun applyNow(
        applying: Applying,
        attempt: Int,
    ) {
        applied = Applied.Active(applying.evaluation)
        val baseline = baselineFor(applying.chats, applying.holidays)

        when (val step = baseline.stepTo(applying)) {
            is Step.First -> {
                refresh(step.value.evaluation)
                keep(Baseline.Known.of(applying))
            }

            is Step.Same -> {
                refresh(step.value.evaluation)
                state = BaselineState.Loaded(baseline)
            }

            is Step.Moved -> {
                held.sweep(applying.evaluation)
                val recheck = held.recheckSnoozed(step.from.map(Applying::evaluation), applying.evaluation)
                requestSilencing()

                when (recheck) {
                    is RecheckStart.Started -> {
                        recheck.job.join()
                        keep(Baseline.Known.of(applying))
                    }

                    is RecheckStart.Skipped -> {
                        keep(step.from.skipped(applying))
                        recheckLater(applying, attempt)
                    }
                }
            }
        }
    }

    private fun recheckLater(
        applying: Applying,
        attempt: Int,
    ) {
        when {
            attempt < RECHECK_RETRIES.size -> {
                scope.launch {
                    delay(RECHECK_RETRIES[attempt])
                    applyLock.withLock {
                        if (applied == Applied.Active(applying.evaluation)) applyNow(applying, attempt + 1)
                    }
                }
            }

            else -> {
                report("recheck still skipped after ${RECHECK_RETRIES.size} retries, waiting for the next change")
            }
        }
    }

    private fun refresh(evaluation: Evaluation) {
        held.sweep(evaluation)
        requestSilencing()
    }

    private suspend fun baselineFor(
        chats: SeenChats,
        holidays: Holidays,
    ): Baseline<Applying> =
        when (val current = state) {
            BaselineState.NotLoaded -> {
                val load = keeper.load()
                report(startupReport(load))

                when (val stored = load.baseline) {
                    Baseline.Unknown -> Baseline.Unknown
                    is Baseline.Known -> stored.map { Applying(it, chats, holidays) }
                }
            }

            is BaselineState.Loaded -> {
                current.baseline
            }
        }

    private fun startupReport(load: AppliedRulesLoad): String =
        when (load) {
            AppliedRulesLoad.Missing -> "no rules applied before, nothing to recheck"
            is AppliedRulesLoad.Loaded -> "comparing with the rules last applied"
            is AppliedRulesLoad.Failed -> "last applied rules unreadable, not rechecking: ${load.detail}"
        }

    private suspend fun keep(baseline: Baseline.Known<Applying>) {
        state = BaselineState.Loaded(baseline)

        when (val save = keeper.save(baseline.map(Applying::rules))) {
            AppliedRulesSave.Saved -> {}

            is AppliedRulesSave.Failed -> {
                report("applied rules not saved: ${save.detail}")
            }
        }
    }
}
