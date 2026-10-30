package dev.danielgilchrist.knockoff.lint

import dev.detekt.api.RuleName
import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

class KnockoffRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSetId = RuleSetId("knockoff")

    override fun instance(): RuleSet =
        RuleSet(
            ruleSetId,
            mapOf(
                RuleName("NoImportAlias") to ::NoImportAlias,
                RuleName("NoTypeFiltering") to ::NoTypeFiltering,
                RuleName("SystemCallOutsideBoundary") to ::SystemCallOutsideBoundary,
                RuleName("UnapprovedCatch") to ::UnapprovedCatch,
                RuleName("UnapprovedThrow") to ::UnapprovedThrow,
                RuleName("UntypedResult") to ::UntypedResult,
                RuleName("UnusedImport") to ::UnusedImport,
            ),
        )
}
