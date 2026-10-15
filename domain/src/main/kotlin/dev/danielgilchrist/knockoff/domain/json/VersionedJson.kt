package dev.danielgilchrist.knockoff.domain.json

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

internal object VersionedJson {
    private const val VERSION = "version"

    val json: Json = Json { prettyPrint = true }

    fun <T> decode(
        text: String,
        currentVersion: Int,
        deserializer: DeserializationStrategy<T>,
    ): VersionedDecode<T> {
        val root =
            try {
                json.parseToJsonElement(text)
            } catch (exception: SerializationException) {
                return VersionedDecode.Unreadable(exception.toString())
            }

        return when (root) {
            is JsonObject -> versioned(root, currentVersion, deserializer)
            else -> VersionedDecode.Unreadable("not a JSON object")
        }
    }

    private fun <T> versioned(
        document: JsonObject,
        currentVersion: Int,
        deserializer: DeserializationStrategy<T>,
    ): VersionedDecode<T> {
        val version =
            when (val element = document[VERSION]) {
                is JsonPrimitive -> if (element.isString) null else element.intOrNull
                else -> null
            }

        return when (version) {
            null -> VersionedDecode.Unreadable("missing or non-integer \"$VERSION\"")
            currentVersion -> current(document, deserializer)
            else -> VersionedDecode.UnsupportedVersion(version)
        }
    }

    private fun <T> current(
        document: JsonObject,
        deserializer: DeserializationStrategy<T>,
    ): VersionedDecode<T> =
        try {
            VersionedDecode.Decoded(json.decodeFromJsonElement(deserializer, document))
        } catch (exception: SerializationException) {
            VersionedDecode.Unreadable(exception.toString())
        }
}
