package dev.danielgilchrist.knockoff.domain.group

sealed interface GroupLabel {
    data class Named(
        val name: GroupName,
    ) : GroupLabel

    data object Unnamed : GroupLabel
}
