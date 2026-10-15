package dev.danielgilchrist.knockoff.storage

sealed interface FileWrite {
    data object Written : FileWrite

    data class Failed(
        val detail: String,
    ) : FileWrite
}
