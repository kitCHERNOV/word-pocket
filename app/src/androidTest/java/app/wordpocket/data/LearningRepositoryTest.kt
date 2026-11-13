package app.wordpocket.data

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.wordpocket.domain.WordDraft
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LearningRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: WordRepository
    private var dictionaryId = 0L

    @Before fun prepare() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Application>(), AppDatabase::class.java)
            .addCallback(AppDatabase.INITIAL_SETTINGS).build()
        repository = WordRepository(database)
        dictionaryId = repository.createDictionary("Слова", 2)
        listOf("cat" to "кот", "dog" to "собака", "sun" to "солнце", "moon" to "луна").forEach { (word, translation) ->
            repository.saveWord(dictionaryId, null, WordDraft(word, translation))
        }
    }

    @After fun cleanup() { database.close() }

    @Test fun `fresh_database_defaults_to_one_correct_answer`() = runBlocking {
        assertEquals(AppSettings(), database.dao().settings())
        repository.startPool(dictionaryId)
        val word = repository.words(dictionaryId).first()
        assertTrue(repository.answerLearning(word.id, true).learned)
        assertTrue(repository.words(dictionaryId).first().learned)
        assertEquals(1, repository.pool(dictionaryId).size)
    }

    @Test fun `threshold_changes_preserve_progress_until_a_future_answer_and_apply_to_every_dictionary`() = runBlocking {
        repository.updateLearningSettings(3)
        repository.startPool(dictionaryId)
        val word = repository.words(dictionaryId).first()
        repeat(2) { repository.answerLearning(word.id, true) }
        repository.updateLearningSettings(1)
        assertEquals(2, repository.words(dictionaryId).first().streak)
        assertFalse(repository.words(dictionaryId).first().learned)
        // A wrong answer still resets the series even though the old series exceeded the new target.
        assertEquals(0, repository.answerLearning(word.id, false).streak)
        assertTrue(repository.answerLearning(word.id, true).learned)
        repository.updateLearningSettings(5)
        assertTrue(repository.words(dictionaryId).first().learned)
        val second = repository.createDictionary("Другой", 1)
        repository.saveWord(second, null, WordDraft("cat", "кот"))
        repository.startPool(second)
        val other = repository.words(second).single()
        repeat(4) { assertFalse(repository.answerLearning(other.id, true).learned) }
        assertTrue(repository.answerLearning(other.id, true).learned)
    }

    @Test fun `known_cards_leave_fixed_pool_without_replacement_and_can_return_to_the_next_pool`() = runBlocking {
        repository.startPool(dictionaryId)
        val original = repository.pool(dictionaryId)
        val first = repository.words(dictionaryId).first()
        val second = repository.createDictionary("Другой", 1)
        repository.saveWord(second, null, first.let { WordDraft(it.word, it.translation) })
        repository.markAlreadyKnown(first.id)
        repository.markAlreadyKnown(first.id)
        val known = repository.words(dictionaryId).first()
        assertTrue(known.alreadyKnown)
        assertFalse(known.learned)
        assertEquals(0, known.streak)
        assertFalse(repository.words(second).single().alreadyKnown)
        repository.startPool(dictionaryId)
        assertEquals(original.drop(1), repository.pool(dictionaryId))
        assertEquals(2, repository.dictionary(dictionaryId)!!.poolTotal)
        repository.returnToLearning(first.id)
        assertFalse(repository.words(dictionaryId).first().alreadyKnown)
        assertEquals(original.drop(1), repository.pool(dictionaryId))
        repository.answerLearning(original.last().wordId, true)
        repository.startPool(dictionaryId)
        assertTrue(repository.pool(dictionaryId).any { it.wordId == first.id })
    }

    @Test fun `known_status_survives_duplicate_import_and_pronunciation_edit_but_not_identity_edit`() = runBlocking {
        val word = repository.words(dictionaryId).first()
        repository.markAlreadyKnown(word.id)
        val duplicate = WordDraft(word.word, word.translation)
        assertEquals(ImportResult(0, 1), repository.importWords(dictionaryId, listOf(duplicate)))
        assertTrue(repository.words(dictionaryId).first().alreadyKnown)
        repository.saveWord(dictionaryId, word.id, duplicate.copy(transcription = "/kæt/"))
        assertTrue(repository.words(dictionaryId).first().alreadyKnown)
        // Converting a learned card to already known must also remove it from review eligibility.
        database.dao().updateWord(repository.words(dictionaryId)[1].copy(learned = true, streak = 1))
        val learned = repository.words(dictionaryId)[1]
        repository.markAlreadyKnown(learned.id)
        assertFalse(repository.words(dictionaryId)[1].learned)
        repository.saveWord(dictionaryId, word.id, duplicate.copy(translation = "кошка"))
        val edited = repository.words(dictionaryId).first()
        assertFalse(edited.alreadyKnown)
        assertFalse(edited.learned)
        assertEquals(0, edited.streak)
    }

    @Test fun `invalid_setting_does_not_overwrite_saved_value`() {
        for (invalid in listOf(0, -1, 101, Int.MAX_VALUE)) {
            assertThrows(IllegalArgumentException::class.java) { runBlocking { repository.updateLearningSettings(invalid) } }
        }
        assertEquals(1, runBlocking { repository.learningSettings().requiredStreak })
    }
}
