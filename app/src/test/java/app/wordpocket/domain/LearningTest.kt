package app.wordpocket.domain

import org.junit.Assert.*
import org.junit.Test

class LearningTest {
    @Test fun `configured number of consecutive correct recalls is required`() {
        for (required in listOf(1, 3, 100)) {
            var result = LearningResult(0, false)
            repeat(required - 1) { result = recordAnswer(result.streak, true, required); assertFalse(result.learned) }
            result = recordAnswer(result.streak, true, required)
            assertEquals(LearningResult(required, true), result)
        }
    }

    @Test fun `wrong answer resets consecutive recalls`() {
        val failed = recordAnswer(2, false, 3)
        assertEquals(LearningResult(0, false), failed)
        assertFalse(recordAnswer(failed.streak, true, 3).learned)
    }

    @Test fun `lower threshold is applied on the next answer`() {
        assertEquals(LearningResult(1, true), recordAnswer(2, true, 1))
        assertEquals(LearningResult(0, false), recordAnswer(2, false, 1))
        assertEquals(LearningResult(3, false), recordAnswer(2, true, 5))
    }

    @Test fun `invalid thresholds are rejected`() {
        for (required in listOf(0, -1, 101, Int.MAX_VALUE)) {
            assertThrows(IllegalArgumentException::class.java) { recordAnswer(0, true, required) }
        }
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
