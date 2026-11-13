package app.wordpocket.data

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.wordpocket.domain.WordDraft
import app.wordpocket.domain.wordIdentity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val name = "migration-${UUID.randomUUID()}.db"
    private var database: AppDatabase? = null

    private fun open(): AppDatabase = Room.databaseBuilder(application, AppDatabase::class.java, name)
        .addMigrations(AppDatabase.MIGRATION_1_2).addCallback(AppDatabase.INITIAL_SETTINGS).build().also { database = it }

    @After fun cleanup() {
        database?.close()
        application.deleteDatabase(name)
    }

    @Test fun `version_one_migrates_without_losing_dictionaries_series_learned_cards_or_active_pool`() = runBlocking {
        val file = application.getDatabasePath(name)
        file.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            // Frozen v1 table definitions from app/schemas/.../1.json, including indexes and identity.
            old.execSQL("CREATE TABLE dictionaries (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, poolSize INTEGER NOT NULL, poolTotal INTEGER NOT NULL)")
            old.execSQL("CREATE TABLE words (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, dictionaryId INTEGER NOT NULL, word TEXT NOT NULL, translation TEXT NOT NULL, transcription TEXT NOT NULL, identity TEXT NOT NULL, streak INTEGER NOT NULL, learned INTEGER NOT NULL, FOREIGN KEY(dictionaryId) REFERENCES dictionaries(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            old.execSQL("CREATE INDEX index_words_dictionaryId ON words(dictionaryId)")
            old.execSQL("CREATE UNIQUE INDEX index_words_dictionaryId_identity ON words(dictionaryId, identity)")
            old.execSQL("CREATE TABLE pool (wordId INTEGER NOT NULL, dictionaryId INTEGER NOT NULL, turn INTEGER NOT NULL, PRIMARY KEY(wordId), FOREIGN KEY(dictionaryId) REFERENCES dictionaries(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(wordId) REFERENCES words(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            old.execSQL("CREATE INDEX index_pool_dictionaryId ON pool(dictionaryId)")
            old.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
            old.execSQL("INSERT INTO room_master_table VALUES (42, '06482786d5efe57b6f4543f532e62cfb')")
            old.execSQL("INSERT INTO dictionaries VALUES (1, 'Первый', 2, 2), (2, 'Второй', 1, 0)")
            old.execSQL("INSERT INTO words VALUES (1, 1, 'cat', 'кот', '/kæt/', ?, 2, 0)", arrayOf(wordIdentity("cat", "кот")))
            old.execSQL("INSERT INTO words VALUES (2, 1, 'dog', 'собака', '', ?, 3, 1)", arrayOf(wordIdentity("dog", "собака")))
            old.execSQL("INSERT INTO words VALUES (3, 1, 'sun', 'солнце', '', ?, 0, 0)", arrayOf(wordIdentity("sun", "солнце")))
            old.execSQL("INSERT INTO words VALUES (4, 2, 'cat', 'кот', '', ?, 0, 0)", arrayOf(wordIdentity("cat", "кот")))
            old.execSQL("INSERT INTO pool VALUES (1, 1, 7), (3, 1, 8)")
            old.version = 1
        }
        val repository = WordRepository(open())
        assertEquals(DictionaryEntity(1, "Первый", 2, 2), repository.dictionary(1))
        assertEquals(DictionaryEntity(2, "Второй", 1, 0), repository.dictionary(2))
        assertEquals(listOf(PoolItem(1, 1, 7), PoolItem(3, 1, 8)), repository.pool(1))
        val words = repository.words(1)
        assertEquals(WordEntity(1, 1, "cat", "кот", "/kæt/", wordIdentity("cat", "кот"), 2, false), words[0])
        assertTrue(words[1].learned)
        assertEquals(3, words[1].streak)
        assertTrue(words.none { it.alreadyKnown })
        assertFalse(repository.words(2).single().alreadyKnown)
        assertEquals(AppSettings(), database!!.dao().settings())
        // The new default does not retroactively mark the existing series as learned.
        assertFalse(words[0].learned)
        assertTrue(repository.answerLearning(words[0].id, true).learned)
        assertEquals(listOf(PoolItem(3, 1, 8)), repository.pool(1))
    }

    @Test fun `settings_known_cards_and_remaining_pool_survive_database_reopen`() = runBlocking {
        var repository = WordRepository(open())
        val dictionaryId = repository.createDictionary("Слова", 2)
        repository.saveWord(dictionaryId, null, WordDraft("cat", "кот"))
        repository.saveWord(dictionaryId, null, WordDraft("dog", "собака"))
        repository.startPool(dictionaryId)
        val first = repository.words(dictionaryId).first()
        repository.markAlreadyKnown(first.id)
        repository.updateLearningSettings(100)
        val remaining = repository.pool(dictionaryId)
        database!!.close()
        repository = WordRepository(open())
        assertEquals(100, repository.learningSettings().requiredStreak)
        assertTrue(repository.words(dictionaryId).first().alreadyKnown)
        assertEquals(remaining, repository.pool(dictionaryId))
        repository.returnToLearning(first.id)
        assertEquals(remaining, repository.pool(dictionaryId))
    }
}
