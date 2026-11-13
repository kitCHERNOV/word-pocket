package app.wordpocket.domain

import java.text.Normalizer
import java.util.Locale

const val DEFAULT_REQUIRED_STREAK = 1
val REQUIRED_STREAK_RANGE = 1..100

fun normalizeAnswer(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKC)
    .trim().lowercase(Locale.ROOT)
    .replace('’', '\'').replace('‘', '\'')
    .replace(Regex("\\s+"), " ")

fun answersMatch(answer: String, expected: String): Boolean =
    normalizeAnswer(answer).isNotEmpty() && normalizeAnswer(answer) == normalizeAnswer(expected)

fun wordIdentity(word: String, translation: String): String =
    normalizeAnswer(word) + "\u0000" + normalizeAnswer(translation)

data class LearningResult(val streak: Int, val learned: Boolean)

fun recordAnswer(streak: Int, correct: Boolean, requiredStreak: Int): LearningResult {
    require(requiredStreak in REQUIRED_STREAK_RANGE) { "Количество ответов: от 1 до 100." }
    val next = if (correct) (streak + 1).coerceAtMost(requiredStreak) else 0
    return LearningResult(next, next >= requiredStreak)
}

data class WordDraft(val word: String, val translation: String, val transcription: String = "") {
    fun validated(): WordDraft {
        val cleaned = copy(word = word.trim(), translation = translation.trim(), transcription = transcription.trim())
        require(cleaned.word.isNotBlank()) { "Заполните слово или фразу." }
        require(cleaned.translation.isNotBlank()) { "Заполните перевод." }
        require(cleaned.word.length <= 300) { "Слово или фраза: максимум 300 символов." }
        require(cleaned.translation.length <= 500) { "Перевод: максимум 500 символов." }
        require(cleaned.transcription.length <= 300) { "Транскрипция: максимум 300 символов." }
        require(listOf(cleaned.word, cleaned.translation, cleaned.transcription).none { '\u0000' in it }) { "Недопустимый символ в тексте." }
        return cleaned
    }
}

/** Exclude identical answers and alternate meanings of the prompt: never offer two valid choices. */
fun buildChoices(correct: WordDraft, candidates: List<WordDraft>): List<String> {
    val wrong = candidates.filter {
        normalizeAnswer(it.word) != normalizeAnswer(correct.word) &&
            normalizeAnswer(it.translation) != normalizeAnswer(correct.translation)
    }.distinctBy { normalizeAnswer(it.word) }.shuffled().take(3).map { it.word }
    return if (wrong.size == 3) (wrong + correct.word).shuffled() else emptyList()
}
