package app.wordpocket.data

import org.junit.Assert.*
import org.junit.Test

class WordJsonTest {
    @Test fun `word phrase and optional transcription import`() {
        val result = WordJson.parse("""{"words":[{"word":"journey","translation":"путешествие","transcription":"/ˈdʒɜːni/"},{"word":"How are you?","translation":"Как дела?"}]}""")
        assertEquals(2, result.size)
        assertEquals("/ˈdʒɜːni/", result[0].transcription)
        assertEquals("", result[1].transcription)
    }

    @Test fun `malformed or incomplete files are rejected before insertion`() {
        listOf("not json", "[]", "{}", """{"words":[]}""",
            """{"words":[{"word":"cat","translation":"кот"},{"word":"dog"}]}""",
            """{"words":[{"word":42,"translation":"кот"}]}""",
            """{"words":[{"word":"cat","translation":"кот","transcription":null}]}"""
        ).forEach { json -> assertThrows(IllegalArgumentException::class.java) { WordJson.parse(json) } }
    }

    @Test fun `utf8 byte order mark is accepted`() {
        val bytes = "\uFEFF{\"words\":[{\"word\":\"cat\",\"translation\":\"кот\"}]}".toByteArray()
        assertEquals("cat", WordJson.read(bytes.inputStream()).single().word)
    }

    @Test fun `oversized files are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { WordJson.read(ByteArray(WordJson.MAX_BYTES + 1).inputStream()) }
    }

    @Test fun `export preserves unicode phrases and escapes without exporting progress`() {
        val word = WordEntity(id = 8, dictionaryId = 3, word = "Say \"hello\"", translation = "Скажи «привет»", transcription = "/həˈləʊ/", identity = "test", learned = true, streak = 3)
        val json = WordJson.export(listOf(word))
        val imported = WordJson.parse(json).single()
        assertEquals(word.word, imported.word)
        assertEquals(word.translation, imported.translation)
        assertEquals(word.transcription, imported.transcription)
        assertFalse(json.contains("learned"))
        assertFalse(json.contains("dictionaryId"))
    }
}
