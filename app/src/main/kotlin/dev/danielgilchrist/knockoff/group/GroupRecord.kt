package dev.danielgilchrist.knockoff.group

sealed interface GroupRecord {
    data object Recorded : GroupRecord

    data object Unchanged : GroupRecord

    data class Failed(
        val detail: String,
    ) : GroupRecord
}
