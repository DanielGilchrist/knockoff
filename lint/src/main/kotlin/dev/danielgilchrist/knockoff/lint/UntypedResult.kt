package dev.danielgilchrist.knockoff.lint

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtSimpleNameExpression

class UntypedResult(
    config: Config,
) : Rule(config, "kotlin.Result and runCatching carry an untyped Throwable. Return a sealed result instead.") {
    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
        super.visitSimpleNameExpression(expression)

        val message =
            when (expression.getReferencedName()) {
                "Result" -> {
                    "Result carries an untyped Throwable. Return a sealed result whose variants name each failure."
                }

                "runCatching" -> {
                    "runCatching catches every Throwable, including CancellationException. Catch the specific " +
                        "exception at the boundary and return a sealed result."
                }

                else -> {
                    return
                }
            }

        report(Finding(Entity.from(expression), message))
    }
}
