package app.wordpocket.data

import app.wordpocket.domain.WordDraft
import kotlinx.serialization.json.*
import java.io.InputStream

object WordJson {
    const val MAX_BYTES = 2 * 1024 * 1024
    private const val MAX_WORDS = 10_000

    fun read(stream: InputStream): List<WordDraft> {
        val bytes = stream.readBytesLimited(MAX_BYTES)
        return parse(bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF"))
    }

    fun parse(text: String): List<WordDraft> {
        val root = try { Json.parseToJsonElement(text) } catch (_: Exception) {
            throw IllegalArgumentException("Файл содержит некорректный JSON.")
        }
        require(root is JsonObject) { "Ожидается объект с массивом words." }
        val words = root["words"] as? JsonArray ?: throw IllegalArgumentException("В JSON нужен массив words.")
        require(words.isNotEmpty()) { "Массив words пуст." }
        require(words.size <= MAX_WORDS) { "За один раз можно загрузить до 10 000 карточек." }
        return words.mapIndexed { index, element ->
            try {
                val obj = element as? JsonObject ?: throw IllegalArgumentException("Карточка должна быть объектом.")
                WordDraft(obj.string("word"), obj.string("translation"), obj.string("transcription", optional = true)).validated()
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Карточка ${index + 1}: ${e.message}")
            }
        }
    }

    fun export(words: List<WordEntity>): String = Json { prettyPrint = true }.encodeToString(
        JsonElement.serializer(),
        buildJsonObject {
            put("words", buildJsonArray {
                words.forEach { word -> add(buildJsonObject {
                    put("word", word.word)
                    put("translation", word.translation)
                    if (word.transcription.isNotBlank()) put("transcription", word.transcription)
                }) }
            })
        },
    )

    private fun JsonObject.string(key: String, optional: Boolean = false): String {
        val value = get(key)
        if (value == null && optional) return ""
        require(value is JsonPrimitive && value.isString) { "Поле $key должно быть строкой." }
        return value.content
    }

    private fun InputStream.readBytesLimited(limit: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            require(output.size() + count <= limit) { "Размер JSON не должен превышать 2 МБ." }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }
}
