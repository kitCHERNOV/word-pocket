package app.wordpocket.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.wordpocket.WordPocketApp
import app.wordpocket.data.*
import app.wordpocket.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class TrainingMode { LEARN, REVIEW }
enum class AnswerMode { CHOICES, WRITING }
data class Feedback(val correct: Boolean, val streak: Int, val mastered: Boolean)
data class Training(
    val dictionaryId: Long,
    val dictionaryName: String,
    val mode: TrainingMode,
    val total: Int,
    val remaining: Int,
    val question: WordEntity? = null,
    val choices: List<String> = emptyList(),
    val answerMode: AnswerMode = AnswerMode.CHOICES,
    val feedback: Feedback? = null,
    val completed: Boolean = false,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as WordPocketApp).repository
    val library = repository.library.stateIn(viewModelScope, SharingStarted.Eagerly, Library())
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _training = MutableStateFlow<Training?>(null)
    val training = _training.asStateFlow()
    private val messagesChannel = Channel<String>(Channel.BUFFERED)
    val messages = messagesChannel.receiveAsFlow()
    private val reviewQueue = ArrayDeque<Long>()

    private fun task(block: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try { block() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { messagesChannel.send(e.message ?: "Не удалось выполнить действие.") }
            finally { _busy.value = false }
        }
    }

    fun createDictionary(name: String, onCreated: (Long) -> Unit) = task { onCreated(repository.createDictionary(name)) }
    fun updateDictionary(id: Long, name: String, size: Int, onSaved: () -> Unit) = task {
        repository.updateDictionary(id, name, size); onSaved()
    }
    fun saveWord(dictionaryId: Long, id: Long?, value: WordDraft, onSaved: () -> Unit) = task {
        repository.saveWord(dictionaryId, id, value); onSaved()
    }
    fun deleteDictionary(id: Long, onDeleted: () -> Unit) = task { repository.deleteDictionary(id); onDeleted() }
    fun deleteWord(id: Long, onDeleted: () -> Unit) = task { repository.deleteWord(id); onDeleted() }

    fun importJson(dictionaryId: Long, uri: Uri) = task {
        val drafts = withContext(Dispatchers.IO) {
            getApplication<Application>().contentResolver.openInputStream(uri)?.use { WordJson.read(it) }
                ?: throw IllegalArgumentException("Не удалось открыть файл.")
        }
        val result = repository.importWords(dictionaryId, drafts)
        messagesChannel.send("Добавлено: ${result.added}. Дубликатов пропущено: ${result.skipped}.")
    }

    fun exportJson(dictionaryId: Long, uri: Uri) = task {
        val words = repository.words(dictionaryId)
        withContext(Dispatchers.IO) {
            getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use {
                it.write(WordJson.export(words))
            } ?: throw IllegalArgumentException("Не удалось сохранить файл.")
        }
        messagesChannel.send("Словарь сохранён в JSON. Прогресс обучения хранится отдельно на телефоне.")
    }

    fun startTraining(dictionaryId: Long, mode: TrainingMode) = task {
        if (mode == TrainingMode.LEARN) repository.startPool(dictionaryId)
        val dictionary = requireNotNull(repository.dictionary(dictionaryId)) { "Словарь не найден." }
        val words = repository.words(dictionaryId)
        reviewQueue.clear()
        if (mode == TrainingMode.REVIEW) reviewQueue.addAll(words.filter { it.learned }.shuffled().map { it.id })
        val count = if (mode == TrainingMode.LEARN) repository.pool(dictionaryId).size else reviewQueue.size
        require(count > 0) { if (mode == TrainingMode.LEARN) "Все слова уже выучены." else "В этом словаре пока нет выученных слов." }
        _training.value = Training(dictionaryId, dictionary.name, mode,
            total = if (mode == TrainingMode.LEARN) dictionary.poolTotal else count, remaining = count)
        loadQuestion()
    }

    private suspend fun loadQuestion() {
        val session = _training.value ?: return
        val words = repository.words(session.dictionaryId)
        val pool = if (session.mode == TrainingMode.LEARN) repository.pool(session.dictionaryId) else emptyList()
        val id = if (session.mode == TrainingMode.LEARN) pool.firstOrNull()?.wordId else reviewQueue.firstOrNull()
        val word = words.find { it.id == id }
        val remaining = if (session.mode == TrainingMode.LEARN) pool.size else reviewQueue.size
        val choices = word?.let { correct -> buildChoices(correct.draft(), words.map { it.draft() }) }.orEmpty()
        _training.value = session.copy(question = word, choices = choices, remaining = remaining,
            feedback = null, completed = word == null)
    }

    fun setAnswerMode(mode: AnswerMode) {
        if (!_busy.value) _training.update { it?.takeIf { s -> s.feedback == null }?.copy(answerMode = mode) ?: it }
    }

    fun answer(text: String?) = task {
        val session = _training.value ?: return@task
        val word = session.question ?: return@task
        if (session.feedback != null) return@task
        val correct = text != null && answersMatch(text, word.word)
        val result = if (session.mode == TrainingMode.LEARN) repository.answerLearning(word.id, correct)
        else {
            reviewQueue.removeFirst()
            if (!correct) reviewQueue.addLast(word.id)
            LearningResult(word.streak, false)
        }
        _training.value = session.copy(feedback = Feedback(correct, result.streak, result.learned))
    }

    fun nextQuestion() = task { loadQuestion() }
    fun closeTraining() { if (!_busy.value) { _training.value = null; reviewQueue.clear() } }
}

fun WordEntity.draft() = WordDraft(word, translation, transcription)
