package dev.danielgilchrist.knockoff.ui.editor

data class DiscoveryFacts(
    val label: String,
    val kind: AppKind,
    val accounts: AccountChoices,
)
