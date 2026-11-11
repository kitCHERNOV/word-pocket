package app.wordpocket.domain

import org.junit.Assert.*
import org.junit.Test

class LearningTest {
    @Test fun `three consecutive correct recalls are required`() {
        var result = LearningResult(0, false)
        repeat(2) { result = recordAnswer(result.streak, true); assertFalse(result.learned) }
        result = recordAnswer(result.streak, true)
        assertTrue(result.learned)
        assertEquals(3, result.streak)
    }

    @Test fun `wrong answer resets consecutive recalls`() {
        val failed = recordAnswer(2, false)
        assertEquals(LearningResult(0, false), failed)
        assertFalse(recordAnswer(failed.streak, true).learned)
    }

    @Test fun `phrases accept case whitespace and typographic apostrophes`() {
        assertTrue(answersMatch("  I  DON’T\nknow  ", "I don't know"))
        assertFalse(answersMatch("", "journey"))
        assertFalse(answersMatch("journy", "journey"))
        assertFalse(answersMatch("How are you", "How are you?"))
    }

    @Test fun `identity keeps different meanings distinct`() {
        assertEquals(wordIdentity(" Ticket ", "Билет"), wordIdentity("ticket", "билет"))
        assertNotEquals(wordIdentity("bank", "банк"), wordIdentity("bank", "берег"))
    }

    @Test fun `choices never contain duplicate answers or same translation`() {
        val correct = WordDraft("hello", "привет")
        val candidates = listOf(correct, WordDraft("HELLO", "здравствуйте"), WordDraft("hi", "Привет"),
            WordDraft("ticket", "билет"), WordDraft("hotel", "отель"), WordDraft("water", "вода"))
        repeat(10) {
            val options = buildChoices(correct, candidates)
            assertEquals(4, options.size)
            assertEquals(setOf("hello", "ticket", "hotel", "water"), options.toSet())
        }
    }

    @Test fun `too few distinct distractors requires writing mode`() {
        val word = WordDraft("hello", "привет")
        assertTrue(buildChoices(word, listOf(word, WordDraft("hi", "привет"), WordDraft("ticket", "билет"))).isEmpty())
    }

    @Test fun `empty translation is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { WordDraft("word", " ").validated() }
    }
}
