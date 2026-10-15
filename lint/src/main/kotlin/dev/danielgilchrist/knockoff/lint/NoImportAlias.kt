package dev.danielgilchrist.knockoff.lint

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtImportDirective

class NoImportAlias(
    config: Config,
) : Rule(
        config,
        "Import aliases hide the real name from name-based rules. Import the declaration under its own name.",
    ) {
    override fun visitImportDirective(importDirective: KtImportDirective) {
        super.visitImportDirective(importDirective)

        val alias = importDirective.aliasName ?: return

        report(
            Finding(
                Entity.from(importDirective),
                "Importing ${importDirective.importedFqName} as $alias hides it from name-based rules.",
            ),
        )
    }
}
