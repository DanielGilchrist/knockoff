package dev.danielgilchrist.knockoff.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.StandardCopyOption

class StagedFile(
    private val file: File,
) {
    private val staged = File(file.parentFile, "${file.name}.tmp")

    suspend fun read(): FileRead =
        withContext(Dispatchers.IO) {
            val bytes =
                try {
                    Files.readAllBytes(file.toPath())
                } catch (_: NoSuchFileException) {
                    return@withContext FileRead.Missing
                } catch (exception: IOException) {
                    return@withContext FileRead.Failed(exception.toString())
                }

            try {
                FileRead.Text(bytes.decodeToString(throwOnInvalidSequence = true))
            } catch (exception: CharacterCodingException) {
                FileRead.Undecodable(exception.toString())
            }
        }

    suspend fun write(text: String): FileWrite =
        withContext(Dispatchers.IO) {
            try {
                FileOutputStream(staged).use { output ->
                    output.write(text.encodeToByteArray())
                    output.fd.sync()
                }
                Files.move(staged.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE)

                FileWrite.Written
            } catch (exception: IOException) {
                FileWrite.Failed(exception.toString())
            }
        }
}
