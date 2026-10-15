package dev.danielgilchrist.knockoff.lint

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPostfixExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.kotlin.psi.psiUtil.parents

class UnapprovedThrow(
    config: Config,
) : Rule(config, "Only throw from declarations this module approves, with a justification, in its detekt config.") {
    companion object {
        private val THROWING_CALLS = setOf("require", "requireNotNull", "check", "checkNotNull", "error", "TODO")
    }

    private val allowedDeclarations: List<String> by config(emptyList())

    override fun visitThrowExpression(expression: KtThrowExpression) {
        super.visitThrowExpression(expression)
        inspect(expression, "throw")
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)

        val callee = expression.calleeExpression?.text

        if (callee in THROWING_CALLS) {
            inspect(expression, "$callee()")
        }
    }

    override fun visitCallableReferenceExpression(expression: KtCallableReferenceExpression) {
        super.visitCallableReferenceExpression(expression)

        val name = expression.callableReference.getReferencedName()

        if (name in THROWING_CALLS) {
            inspect(expression, "::$name")
        }
    }

    override fun visitPostfixExpression(expression: KtPostfixExpression) {
        super.visitPostfixExpression(expression)

        if (expression.operationToken == KtTokens.EXCLEXCL) {
            inspect(expression, "!!")
        }
    }

    private fun inspect(
        element: KtElement,
        throwing: String,
    ) {
        val owner =
            element
                .parents
                .filterIsInstance<KtNamedDeclaration>()
                .firstNotNullOfOrNull { declaration ->
                    declaration.fqName?.let { signature(declaration, it.asString()) }
                }

        when {
            owner == null -> {
                report(Finding(Entity.from(element), "$throwing outside a named declaration can't be approved."))
            }

            owner !in allowedDeclarations -> {
                report(
                    Finding(
                        Entity.from(element),
                        "$throwing in $owner isn't approved. Model the failure as a type, or approve $owner with a " +
                            "justification under allowedDeclarations if it guards a genuine bug that can't be typed.",
                    ),
                )
            }
        }
    }

    private fun signature(
        declaration: KtNamedDeclaration,
        name: String,
    ): String =
        when (declaration) {
            is KtNamedFunction -> {
                val receiver = declaration.receiverTypeReference?.let { listOf("receiver: ${it.text}") }.orEmpty()
                val parameters =
                    declaration.valueParameters.map { parameter ->
                        listOfNotNull(
                            "vararg".takeIf { parameter.isVarArg },
                            parameter.typeReference?.text,
                        ).joinToString(" ")
                    }

                "$name(${(receiver + parameters).joinToString()})"
            }

            is KtProperty -> {
                declaration.receiverTypeReference?.let { "$name(receiver: ${it.text})" } ?: name
            }

            else -> {
                name
            }
        }
}
