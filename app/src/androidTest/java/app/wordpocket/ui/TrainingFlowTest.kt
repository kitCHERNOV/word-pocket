package app.wordpocket.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import java.io.File
import androidx.activity.ComponentActivity
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.wordpocket.data.*
import app.wordpocket.domain.WordDraft
import app.wordpocket.domain.LearningResult
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrainingFlowTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var database: AppDatabase
    private lateinit var repository: FailingRepository
    private lateinit var vm: AppViewModel
    private val store = ViewModelStore()
    private var dictionaryId = 0L
    private val darkTheme = mutableStateOf(false)
    private val fontScale = mutableStateOf(1f)
    private val viewportWidth = mutableStateOf(720.dp)

    @Before fun prepare() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(application, AppDatabase::class.java).build()
        repository = FailingRepository(database)
        runBlocking {
            dictionaryId = repository.createDictionary("На каждый день", 1)
            listOf("cat" to "кот", "dog" to "собака", "sun" to "солнце", "moon" to "луна").forEach { (word, translation) ->
                repository.saveWord(dictionaryId, null, WordDraft(word, translation, "/$word/"))
            }
        }
        compose.runOnUiThread {
            val owner = object : ViewModelStoreOwner { override val viewModelStore = store }
            vm = ViewModelProvider(owner, object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(application, repository) as T
            })[AppViewModel::class.java]
        }
        compose.setContent {
            WordPocketTheme(darkTheme = darkTheme.value) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale.value)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        Surface(Modifier.widthIn(max = viewportWidth.value).fillMaxSize().testTag("app-content"),
                            color = MaterialTheme.colorScheme.background) { WordPocketRoot(vm) }
                    }
                }
            }
        }
    }

    @After fun cleanup() {
        compose.runOnUiThread { store.clear() }
        database.close()
    }

    private fun awaitState(timeoutMillis: Long = 10_000, condition: () -> Boolean) {
        try {
            compose.waitUntil(timeoutMillis) {
                // Drain Android's main looper as Room resumes ViewModel coroutines on it.
                compose.waitForIdle()
                condition()
            }
            compose.waitForIdle()
        } catch (error: ComposeTimeoutException) {
            throw AssertionError("busy=${vm.busy.value}; training=${vm.training.value}", error)
        }
    }

    private fun start(mode: TrainingMode = TrainingMode.LEARN) {
        compose.runOnUiThread { vm.startTraining(dictionaryId, mode) }
        awaitState(10_000) { !vm.busy.value && vm.training.value?.question != null }
    }

    private fun submit(text: String?, source: AnswerMode = AnswerMode.CHOICES) {
        val token = vm.training.value!!.questionToken
        compose.runOnUiThread { vm.answer(token, text, source) }
        awaitState(10_000) { !vm.busy.value }
    }

    @Test fun correctChoicesAdvanceAndMasterSingletonWithoutContinuation() {
        start()
        repeat(3) { index ->
            val before = vm.training.value!!
            compose.onNodeWithTag("choice-cat").performScrollTo().performClick()
            awaitState(10_000) { !vm.busy.value && vm.training.value!!.questionToken > before.questionToken }
            compose.onNodeWithText("Продолжить").assertDoesNotExist()
            compose.onNodeWithTag("answer-feedback").assertDoesNotExist()
            val after = vm.training.value!!
            assertNull(after.feedback)
            if (index < 2) {
                assertEquals(before.question!!.id, after.question!!.id)
                assertEquals(index + 1, after.question.streak)
                assertEquals(1, after.remaining)
            } else {
                assertTrue(after.completed)
                assertEquals(0, after.remaining)
                compose.onNodeWithText("Вернуться в словарь").assertIsDisplayed()
            }
            // Simulate a delayed click captured by the old composition.
            compose.runOnUiThread { vm.answer(before.questionToken, "cat", AnswerMode.CHOICES) }
            awaitState(10_000) { !vm.busy.value }
            assertEquals(after, vm.training.value)
        }
        runBlocking {
            val word = repository.words(dictionaryId).first()
            assertTrue(word.learned)
            assertEquals(3, word.streak)
            assertTrue(repository.pool(dictionaryId).isEmpty())
        }
    }

    @Test fun wrongChoiceAndUnknownKeepManualFeedbackAndResetStreak() {
        start()
        submit("cat")
        val before = vm.training.value!!
        compose.onNodeWithTag("choice-dog").performScrollTo().performClick()
        awaitState(10_000) { !vm.busy.value && vm.training.value?.feedback != null }
        assertEquals(before.questionToken, vm.training.value!!.questionToken)
        assertEquals("dog", vm.training.value!!.feedback!!.selectedAnswer)
        assertEquals(0, vm.training.value!!.feedback!!.streak)
        compose.onNodeWithTag("answer-feedback").assertExists()
        compose.onNodeWithText("Продолжить").assertIsDisplayed().performClick()
        awaitState(10_000) { !vm.busy.value && vm.training.value!!.questionToken > before.questionToken }
        val next = vm.training.value!!
        compose.runOnUiThread { vm.nextQuestion(before.questionToken) }
        awaitState(10_000) { !vm.busy.value }
        assertEquals(next, vm.training.value)
        submit(null)
        assertFalse(vm.training.value!!.feedback!!.correct)
        compose.onNodeWithText("Продолжить").assertIsDisplayed()
    }

    @Test fun correctWrittenAnswerRequiresContinuation() {
        start()
        compose.runOnUiThread { vm.setAnswerMode(AnswerMode.WRITING) }
        compose.onNodeWithTag("written-answer").performScrollTo().performTextInput("  CAT  ")
        compose.onNodeWithText("Проверить").performScrollTo().performClick()
        awaitState(10_000) { !vm.busy.value && vm.training.value?.feedback != null }
        assertTrue(vm.training.value!!.feedback!!.correct)
        compose.onNodeWithText("Продолжить").assertExists()
        compose.onNodeWithText("/cat/").assertExists()
    }

    @Test fun reviewAdvancesImmediatelyAndWrongAnswerPreservesLearnedStatus() {
        runBlocking {
            val word = repository.words(dictionaryId).first()
            database.dao().updateWord(word.copy(learned = true, streak = 3))
        }
        start(TrainingMode.REVIEW)
        submit("dog")
        assertFalse(vm.training.value!!.feedback!!.correct)
        assertTrue(runBlocking { repository.words(dictionaryId).first().learned })
        val token = vm.training.value!!.questionToken
        compose.runOnUiThread { vm.nextQuestion(token) }
        awaitState(10_000) { !vm.busy.value && vm.training.value!!.questionToken > token }
        submit("cat")
        assertTrue(vm.training.value!!.completed)
        compose.onNodeWithText("Продолжить").assertDoesNotExist()
        assertTrue(runBlocking { repository.words(dictionaryId).first().learned })
    }

    @Test fun insufficientChoicesUsesWritingAndSaveFailureDoesNotAdvance() {
        runBlocking { repository.words(dictionaryId).drop(1).forEach { repository.deleteWord(it.id) } }
        start()
        assertTrue(vm.training.value!!.choices.isEmpty())
        compose.onNodeWithTag("written-answer").assertExists()
        val fallback = vm.training.value!!
        submit("cat", AnswerMode.CHOICES)
        assertEquals(fallback, vm.training.value)
        submit("cat", AnswerMode.WRITING)
        assertTrue(vm.training.value!!.feedback!!.correct)
        val token = vm.training.value!!.questionToken
        compose.runOnUiThread { vm.nextQuestion(token) }
        awaitState(10_000) { !vm.busy.value && vm.training.value!!.questionToken > token }
        val before = vm.training.value!!
        // Remove the pending word from storage while retaining the displayed question.
        runBlocking { repository.deleteWord(before.question!!.id) }
        submit("cat", AnswerMode.WRITING)
        assertEquals(before, vm.training.value)
        assertNull(vm.training.value!!.feedback)
        assertFalse(vm.training.value!!.completed)
    }

    @Test fun failedSaveCanBeRetriedAndDuplicateEventsAreIgnored() {
        start()
        val before = vm.training.value!!
        repository.failSave = true
        submit("cat")
        assertEquals(before, vm.training.value)
        assertEquals(0, runBlocking { repository.words(dictionaryId).first().streak })
        compose.onNodeWithText("Тест: запись недоступна").assertExists()
        repository.failSave = false
        compose.runOnUiThread {
            vm.answer(before.questionToken, "cat", AnswerMode.CHOICES)
            vm.answer(before.questionToken, "cat", AnswerMode.CHOICES)
        }
        awaitState(10_000) { !vm.busy.value && vm.training.value!!.questionToken > before.questionToken }
        assertEquals(1, repository.savedAnswers)
        assertEquals(1, vm.training.value!!.question!!.streak)
    }

    @Test fun committedAnswerSurvivesFailedLoadAndRetryDoesNotCountItTwice() {
        start()
        val before = vm.training.value!!
        repository.failNextRead = true
        submit("cat")
        val failed = vm.training.value!!
        assertTrue(failed.advancePending)
        assertTrue(failed.loadFailed)
        assertNull(failed.feedback)
        assertEquals(before.questionToken, failed.questionToken)
        assertEquals(1, runBlocking { repository.words(dictionaryId).first().streak })
        // A late answer for the still displayed token must not repeat the transaction.
        submit("cat")
        assertEquals(1, repository.savedAnswers)
        compose.onNodeWithText("Повторить загрузку").assertIsDisplayed().assertIsEnabled().performClick()
        awaitState(10_000) { !vm.busy.value && vm.training.value!!.questionToken > before.questionToken }
        assertEquals(1, vm.training.value!!.question!!.streak)
        assertEquals(1, repository.savedAnswers)
        assertFalse(vm.training.value!!.loadFailed)
    }

    @Test fun fixedPoolRemovesMasteredWordsWithoutAddingNewOnes() {
        runBlocking { repository.updateDictionary(dictionaryId, "На каждый день", 2) }
        start()
        val original = runBlocking { repository.pool(dictionaryId).map { it.wordId }.toSet() }
        repeat(6) { index ->
            val question = vm.training.value!!.question!!
            assertTrue(question.id in original)
            submit(question.word)
            val pool = runBlocking { repository.pool(dictionaryId) }
            assertTrue(pool.all { it.wordId in original })
            assertEquals(if (index < 4) 2 else 5 - index, pool.size)
        }
        assertTrue(vm.training.value!!.completed)
        assertEquals(2, vm.training.value!!.total)
        val words = runBlocking { repository.words(dictionaryId) }
        assertEquals(original, words.filter { it.learned }.map { it.id }.toSet())
        assertTrue(words.filter { it.id !in original }.all { it.streak == 0 })
    }

    @Test fun reviewChoiceLoadsNextWordBeforeShowingTheResult() {
        runBlocking {
            repository.words(dictionaryId).take(2).forEach { database.dao().updateWord(it.copy(learned = true, streak = 3)) }
        }
        start(TrainingMode.REVIEW)
        val before = vm.training.value!!
        submit(before.question!!.word)
        assertEquals(1, vm.training.value!!.remaining)
        assertNotEquals(before.question.id, vm.training.value!!.question!!.id)
        compose.onNodeWithTag("answer-feedback").assertDoesNotExist()
        submit(vm.training.value!!.question!!.word)
        assertTrue(vm.training.value!!.completed)
    }

    @Test fun writingInputIsClearedForTheNextShowingOfTheSameWord() {
        start()
        compose.runOnUiThread { vm.setAnswerMode(AnswerMode.WRITING) }
        compose.onNodeWithTag("written-answer").performScrollTo().performTextInput("cat")
        compose.onNodeWithText("Проверить").performScrollTo().performClick()
        awaitState(10_000) { !vm.busy.value && vm.training.value!!.feedback != null }
        val before = vm.training.value!!
        compose.onNodeWithText("Продолжить").performClick()
        awaitState(10_000) { !vm.busy.value && vm.training.value!!.questionToken > before.questionToken }
        assertEquals(before.question!!.id, vm.training.value!!.question!!.id)
        assertEquals("", compose.onNodeWithTag("written-answer").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.EditableText].text)
    }

    @Test fun smallScreenWithLargeFontKeepsFiltersAndDialogActionsReachable() {
        awaitState { vm.library.value.dictionaries.isNotEmpty() }
        compose.runOnUiThread {
            viewportWidth.value = 320.dp
            fontScale.value = 2f
        }
        screenshot("library-small-large-font")
        compose.onNodeWithText("На каждый день").performScrollTo().performClick()
        compose.onNodeWithTag("dictionary-list").performScrollToNode(hasText("Выучено"))
        compose.onNodeWithText("Выучено", useUnmergedTree = true).assertIsDisplayed()
        val viewport = compose.onNodeWithTag("app-content").fetchSemanticsNode().boundsInRoot
        val filter = compose.onNodeWithText("Выучено", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(filter.left >= viewport.left && filter.right <= viewport.right)
        screenshot("dictionary-small-large-font")
        compose.onNodeWithContentDescription("Настроить словарь").performClick()
        compose.onNodeWithText("Сохранить").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Отмена").performScrollTo().assertIsDisplayed().performClick()
        start()
        compose.onNodeWithTag("choice-cat").performScrollTo().assertIsDisplayed().performClick()
        awaitState { !vm.busy.value && vm.training.value!!.question!!.streak == 1 }
        screenshot("training-small-large-font")
    }

    @Test fun newDictionaryOffersDefaultPoolAndRejectsOutOfRangeSizes() {
        awaitState { vm.library.value.dictionaries.isNotEmpty() }
        compose.onNodeWithContentDescription("Создать словарь").performClick()
        val size = compose.onNode(hasSetTextAction() and hasText("Карточек в одном пуле"))
        assertEquals("10", size.fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.EditableText].text)
        compose.onNode(hasSetTextAction() and hasText("Название")).performTextInput("Любимые книги")
        for (invalid in listOf("0", "101")) {
            size.performTextReplacement(invalid)
            compose.onNodeWithText("Создать").assertIsNotEnabled()
        }
        size.performTextReplacement("1")
        compose.onNodeWithText("Создать").performScrollTo().assertIsEnabled().performClick()
        awaitState { !vm.busy.value && vm.library.value.dictionaries.any { it.name == "Любимые книги" } }
        assertEquals(1, vm.library.value.dictionaries.first { it.name == "Любимые книги" }.poolSize)
    }
    @Test fun captureDesignStatesInBothThemes() {
        awaitState(10_000) { vm.library.value.dictionaries.isNotEmpty() }
        screenshot("library-light")
        compose.runOnUiThread { darkTheme.value = true }
        screenshot("library-dark")
        compose.runOnUiThread { darkTheme.value = false }
        compose.onNodeWithText("На каждый день").performScrollTo().performClick()
        screenshot("dictionary-light")
        compose.runOnUiThread { darkTheme.value = true }
        screenshot("dictionary-dark")
        compose.runOnUiThread { darkTheme.value = false }
        start()
        screenshot("training-light")
        compose.runOnUiThread { darkTheme.value = true }
        screenshot("training-dark")
        submit("dog")
        screenshot("feedback-dark")
        compose.runOnUiThread { darkTheme.value = false }
        screenshot("feedback-light")
        val token = vm.training.value!!.questionToken
        compose.runOnUiThread { vm.nextQuestion(token) }
        awaitState(10_000) { !vm.busy.value && vm.training.value!!.questionToken > token }
        repeat(3) { submit("cat") }
        screenshot("completed-light")
        compose.runOnUiThread { darkTheme.value = true }
        screenshot("completed-dark")
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val bounds = compose.onNodeWithTag("app-content").fetchSemanticsNode().boundsInWindow
        lateinit var bitmap: Bitmap
        compose.runOnUiThread {
            bitmap = Bitmap.createBitmap(bounds.width.toInt(), bounds.height.toInt(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.translate(-bounds.left, -bounds.top)
            compose.activity.window.decorView.draw(canvas)
        }
        val directory = System.getProperty("wordpocket.screenshotDir")?.let(::File)
            ?: File(compose.activity.getExternalFilesDir(null), "screenshots")
        directory.mkdirs()
        File(directory, "$name.png").outputStream().use {
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        bitmap.recycle()
    }

    private class FailingRepository(database: AppDatabase) : WordRepository(database) {
        var failSave = false
        var failNextRead = false
        var savedAnswers = 0

        override suspend fun words(id: Long): List<WordEntity> {
            if (failNextRead) {
                failNextRead = false
                error("Тест: чтение недоступно")
            }
            return super.words(id)
        }

        override suspend fun answerLearning(wordId: Long, correct: Boolean): LearningResult {
            check(!failSave) { "Тест: запись недоступна" }
            return super.answerLearning(wordId, correct).also { savedAnswers++ }
        }
    }
}
