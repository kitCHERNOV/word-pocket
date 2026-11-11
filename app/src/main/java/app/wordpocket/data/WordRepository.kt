package app.wordpocket.data

import androidx.room.withTransaction
import app.wordpocket.domain.*
import kotlinx.coroutines.flow.combine

data class Library(
    val dictionaries: List<DictionaryEntity> = emptyList(),
    val words: List<WordEntity> = emptyList(),
    val pool: List<PoolItem> = emptyList(),
)
data class ImportResult(val added: Int, val skipped: Int)

class WordRepository(private val database: AppDatabase) {
    private val dao = database.dao()
    val library = combine(dao.observeDictionaries(), dao.observeWords(), dao.observePool()) { dictionaries, words, pool ->
        Library(dictionaries, words, pool)
    }

    suspend fun createDictionary(name: String): Long {
        require(name.trim().isNotEmpty() && name.trim().length <= 80) { "Название должно содержать от 1 до 80 символов." }
        return dao.insertDictionary(DictionaryEntity(name = name.trim()))
    }

    suspend fun updateDictionary(id: Long, name: String, poolSize: Int) {
        require(name.trim().isNotEmpty() && name.trim().length <= 80) { "Название должно содержать от 1 до 80 символов." }
        require(poolSize in 1..100) { "Размер пула: от 1 до 100 слов." }
        database.withTransaction {
            val current = requireNotNull(dao.dictionary(id)) { "Словарь не найден." }
            dao.updateDictionary(current.copy(name = name.trim(), poolSize = poolSize))
        }
    }

    suspend fun saveWord(dictionaryId: Long, id: Long?, draft: WordDraft) {
        val value = draft.validated()
        database.withTransaction {
            requireNotNull(dao.dictionary(dictionaryId)) { "Словарь не найден." }
            val identity = wordIdentity(value.word, value.translation)
            require(dao.words(dictionaryId).none { it.id != id && it.identity == identity }) { "Такая карточка уже есть в словаре." }
            if (id == null) {
                dao.insertWord(WordEntity(dictionaryId = dictionaryId, word = value.word, translation = value.translation, transcription = value.transcription, identity = identity))
            } else {
                val old = requireNotNull(dao.word(id)) { "Карточка не найдена." }
                require(old.dictionaryId == dictionaryId)
                // Editing pronunciation alone does not discard learning progress.
                val changed = identity != old.identity
                dao.updateWord(old.copy(word = value.word, translation = value.translation, transcription = value.transcription,
                    identity = identity, streak = if (changed) 0 else old.streak, learned = if (changed) false else old.learned))
            }
        }
    }

    suspend fun importWords(dictionaryId: Long, drafts: List<WordDraft>): ImportResult = database.withTransaction {
        requireNotNull(dao.dictionary(dictionaryId)) { "Словарь не найден." }
        val valid = drafts.map { it.validated() }
        var added = 0
        valid.forEach { value ->
            val id = dao.insertWord(WordEntity(dictionaryId = dictionaryId, word = value.word,
                translation = value.translation, transcription = value.transcription, identity = wordIdentity(value.word, value.translation)))
            if (id != -1L) added++
        }
        ImportResult(added, drafts.size - added)
    }

    suspend fun deleteDictionary(id: Long) = dao.deleteDictionary(id)
    suspend fun deleteWord(id: Long): Unit = database.withTransaction {
        val word = dao.word(id) ?: return@withTransaction
        val inPool = dao.pool(word.dictionaryId).any { it.wordId == id }
        dao.deleteWord(id)
        if (inPool) dao.dictionary(word.dictionaryId)?.let { dao.updateDictionary(it.copy(poolTotal = (it.poolTotal - 1).coerceAtLeast(0))) }
    }
    suspend fun words(id: Long) = dao.words(id)
    suspend fun dictionary(id: Long) = dao.dictionary(id)
    suspend fun pool(id: Long) = dao.pool(id)

    suspend fun startPool(dictionaryId: Long): Unit = database.withTransaction {
        val dictionary = requireNotNull(dao.dictionary(dictionaryId)) { "Словарь не найден." }
        if (dao.pool(dictionaryId).isEmpty()) {
            val selected = dao.words(dictionaryId).filterNot { it.learned }.take(dictionary.poolSize)
            dao.savePool(selected.mapIndexed { index, word -> PoolItem(word.id, dictionaryId, index.toLong()) })
            dao.updateDictionary(dictionary.copy(poolTotal = selected.size))
        }
    }

    suspend fun answerLearning(wordId: Long, correct: Boolean): LearningResult = database.withTransaction {
        val word = requireNotNull(dao.word(wordId)) { "Карточка не найдена." }
        val pool = dao.pool(word.dictionaryId)
        require(pool.any { it.wordId == wordId }) { "Карточка больше не входит в пул." }
        val result = recordAnswer(word.streak, correct)
        dao.updateWord(word.copy(streak = result.streak, learned = result.learned))
        if (result.learned) dao.removeFromPool(wordId)
        else dao.savePool(listOf(PoolItem(wordId, word.dictionaryId, (pool.maxOfOrNull { it.turn } ?: 0) + 1)))
        result
    }
}
