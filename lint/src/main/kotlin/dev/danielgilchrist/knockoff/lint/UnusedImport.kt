package dev.danielgilchrist.knockoff.lint

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class UnusedImport(
    config: Config,
) : Rule(config, "Remove imports nothing in the file refers to.") {
    companion object {
        private val IMPLICIT =
            setOf(
                "getValue",
                "setValue",
                "provideDelegate",
                "invoke",
                "iterator",
                "next",
                "hasNext",
                "contains",
                "get",
                "set",
                "plus",
                "minus",
                "times",
                "div",
                "rem",
                "rangeTo",
                "rangeUntil",
                "unaryPlus",
                "unaryMinus",
                "not",
                "inc",
                "dec",
                "compareTo",
                "plusAssign",
                "minusAssign",
                "timesAssign",
                "divAssign",
                "remAssign",
            )
        private val COMPONENT = Regex("component\\d+")
    }

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)

        val referenced =
            file
                .collectDescendantsOfType<KtSimpleNameExpression>()
                .filter { it.getStrictParentOfType<KtImportDirective>() == null }
                .map { it.getReferencedName() }
                .toSet()

        file.importDirectives
            .filterNot { it.isAllUnder }
            .forEach { directive ->
                val name = directive.importedName?.asString().orEmpty()

                if (!isUsed(name, referenced)) {
                    report(Finding(Entity.from(directive), "${directive.importedFqName} is imported but never used."))
                }
            }
    }

    private fun isUsed(
        name: String,
        referenced: Set<String>,
    ): Boolean = name in referenced || name in IMPLICIT || COMPONENT.matches(name)
}
