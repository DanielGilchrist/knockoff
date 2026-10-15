package dev.danielgilchrist.knockoff.lint

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCatchClause
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtUserType

class UnapprovedCatch(
    config: Config,
) : Rule(config, "Only catch exception types this module approves, with a justification, in its detekt config.") {
    private val allowedExceptions: List<String> by config(emptyList())

    override fun visitCatchSection(catchClause: KtCatchClause) {
        super.visitCatchSection(catchClause)

        val caught = catchClause.catchParameter?.typeReference?.typeElement

        when {
            caught !is KtUserType -> report(catchClause, "This catch has no named exception type.")
            caught.referencedName !in allowedExceptions -> report(catchClause, unapproved(caught.referencedName))
        }
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        if (expression.calleeExpression?.text == "catch") {
            report(
                expression,
                "A catch operator handles every Throwable except cancellation. Convert specific exceptions to a " +
                    "sealed result where the failure happens.",
            )
        }
    }

    private fun unapproved(type: String?): String =
        "Catching $type isn't approved here. Return a sealed result from the boundary that can fail, or approve the " +
            "type with a justification under allowedExceptions."

    private fun report(
        element: KtElement,
        message: String,
    ) {
        report(Finding(Entity.from(element), message))
    }
}
