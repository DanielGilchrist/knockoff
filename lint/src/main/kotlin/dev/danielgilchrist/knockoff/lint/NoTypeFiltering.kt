package dev.danielgilchrist.knockoff.lint

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtExpression

class NoTypeFiltering(
    config: Config,
) : Rule(
        config,
        "Safe casts and filterIsInstance silently drop variants added later. Use an exhaustive when instead.",
    ) {
    override fun visitBinaryWithTypeRHSExpression(expression: KtBinaryExpressionWithTypeRHS) {
        super.visitBinaryWithTypeRHSExpression(expression)

        if (expression.operationReference.getReferencedNameElementType() == KtTokens.AS_SAFE) {
            report(Finding(Entity.from(expression), "`as?` silently drops other types. Use an exhaustive when."))
        }
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        reportFiltering(expression, expression.calleeExpression?.text)
    }

    override fun visitCallableReferenceExpression(expression: KtCallableReferenceExpression) {
        super.visitCallableReferenceExpression(expression)

        reportFiltering(expression, expression.callableReference.getReferencedName())
    }

    private fun reportFiltering(
        expression: KtExpression,
        name: String?,
    ) {
        if (name?.startsWith("filterIsInstance") == true) {
            report(Finding(Entity.from(expression), "$name silently drops other types. Use an exhaustive when."))
        }
    }
}
