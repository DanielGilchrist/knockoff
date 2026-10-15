package dev.danielgilchrist.knockoff.storage

sealed interface FileRead {
    data class Text(
        val text: String,
    ) : FileRead

    data object Missing : FileRead

    data class Undecodable(
        val detail: String,
    ) : FileRead

    data class Failed(
        val detail: String,
    ) : FileRead
}
