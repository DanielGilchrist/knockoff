package dev.danielgilchrist.knockoff.domain.group

sealed interface GroupNameParse {
    data class Valid(
        val name: GroupName,
    ) : GroupNameParse

    data object Blank : GroupNameParse
}
