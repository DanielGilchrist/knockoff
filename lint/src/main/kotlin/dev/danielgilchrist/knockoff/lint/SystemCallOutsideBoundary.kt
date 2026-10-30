package dev.danielgilchrist.knockoff.lint

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.parents

class SystemCallOutsideBoundary(
    config: Config,
) : Rule(config, "Call system services only inside systemCall, which turns their failures into results.") {
    companion object {
        private const val BOUNDARY = "systemCall"
    }

    private val systemCalls: List<String> by config(emptyList())

    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
        super.visitSimpleNameExpression(expression)

        val name = expression.getReferencedName()

        if (name in systemCalls && !insideBoundary(expression)) {
            report(
                Finding(
                    Entity.from(expression),
                    "$name reaches a system service outside $BOUNDARY. Wrap it in $BOUNDARY { } so a refusal or " +
                        "a binder failure becomes a SystemCall value instead of crashing the app.",
                ),
            )
        }
    }

    private fun insideBoundary(expression: KtSimpleNameExpression): Boolean =
        expression
            .parents
            .filterIsInstance<KtLambdaExpression>()
            .any { lambda ->
                lambda.parents
                    .filterIsInstance<KtCallExpression>()
                    .firstOrNull()
                    ?.calleeExpression
                    ?.text == BOUNDARY
            }
}
