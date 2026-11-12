package app.wordpocket.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.wordpocket.R
import app.wordpocket.domain.answersMatch

@Composable
internal fun TrainingScreen(
    session: Training, busy: Boolean, onMode: (AnswerMode) -> Unit,
    onAnswer: (Long, String?, AnswerMode) -> Unit,
) {
    var answer by rememberSaveable(session.questionToken) { mutableStateOf("") }
    val scroll = key(session.questionToken) { rememberScrollState() }
    val writing = session.answerMode == AnswerMode.WRITING || session.choices.isEmpty()
    val source = if (writing) AnswerMode.WRITING else AnswerMode.CHOICES
    val feedback = session.feedback
    val answering = !busy && feedback == null && !session.advancePending && !session.loadFailed
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = 22.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (session.completed) {
                Completion(session)
            } else if (session.question != null) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Eyebrow(if (session.mode == TrainingMode.LEARN) "Изучаем новое" else "Вспоминаем знакомое", Modifier.weight(1f))
                        Muted("${session.total - session.remaining} / ${session.total} карточек")
                    }
                    ThinProgress(if (session.total == 0) 0f else (session.total - session.remaining).toFloat() / session.total)
                }
                Row(Modifier.widthIn(max = 300.dp).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)).padding(4.dp)) {
                    listOf(AnswerMode.CHOICES to "4 варианта", AnswerMode.WRITING to "Написать").forEach { (mode, label) ->
                        val selected = if (mode == AnswerMode.WRITING) writing else !writing
                        Surface(selected = selected, onClick = { onMode(mode) },
                            enabled = answering && (mode != AnswerMode.CHOICES || session.choices.isNotEmpty()),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(7.dp),
                            color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant) {
                            Box(Modifier.padding(8.dp), contentAlignment = Alignment.Center) { Text(label, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
                val word = session.question
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Column(Modifier.fillMaxWidth().heightIn(min = 220.dp).padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically)) {
                        Eyebrow("Как это по-английски?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(word.translation, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("question"))
                        if (feedback != null) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Text(word.word, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                            if (word.transcription.isNotBlank()) Muted(word.transcription)
                        }
                        if (session.mode == TrainingMode.LEARN) {
                            val streak = feedback?.streak ?: word.streak
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StreakDots(streak)
                                Muted("$streak из 3 верных ответов")
                            }
                        }
                    }
                }
                if (!writing) {
                    Muted("Выберите правильный вариант")
                    session.choices.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { choice ->
                                val correct = feedback != null && answersMatch(choice, word.word)
                                val wrong = feedback?.correct == false && choice == feedback.selectedAnswer
                                val container = when { correct -> MaterialTheme.colorScheme.secondaryContainer; wrong -> MaterialTheme.colorScheme.errorContainer; else -> MaterialTheme.colorScheme.surface }
                                val ink = when { correct -> MaterialTheme.colorScheme.onSecondaryContainer; wrong -> MaterialTheme.colorScheme.onErrorContainer; else -> MaterialTheme.colorScheme.onSurface }
                                Surface(onClick = { onAnswer(session.questionToken, choice, AnswerMode.CHOICES) }, enabled = answering,
                                    modifier = Modifier.weight(1f).heightIn(min = 68.dp).testTag("choice-$choice"),
                                    color = container, contentColor = ink, shape = RoundedCornerShape(11.dp),
                                    border = BorderStroke(1.dp, if (correct || wrong) ink else MaterialTheme.colorScheme.outlineVariant)) {
                                    Text(choice, Modifier.padding(14.dp), style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                } else if (feedback == null) {
                    OutlinedTextField(answer, { answer = it }, Modifier.fillMaxWidth().testTag("written-answer"), enabled = answering,
                        label = { Text("Слово или фраза…") }, textStyle = MaterialTheme.typography.titleLarge, shape = RoundedCornerShape(11.dp),
                        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (answer.isNotBlank() && answering) onAnswer(session.questionToken, answer, AnswerMode.WRITING) }), maxLines = 5)
                    PaperButton("Проверить", { onAnswer(session.questionToken, answer, AnswerMode.WRITING) },
                        Modifier.fillMaxWidth(), answering && answer.isNotBlank(), icon = R.drawable.ic_arrow)
                    if (session.choices.isEmpty()) Muted("В словаре пока недостаточно разных вариантов — ответьте письменно.")
                }
                if (feedback == null && !session.advancePending && !session.loadFailed) {
                    TextButton(onClick = { onAnswer(session.questionToken, null, source) }, enabled = answering, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text("Пока не знаю", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (feedback != null) AnswerFeedback(session, feedback)
                Muted("В вашем темпе. Без спешки.", Modifier.padding(vertical = 4.dp))
            }
            if (session.loadFailed) {
                Text(if (session.advancePending) "Не удалось загрузить карточку. Сохранённый ответ не будет засчитан повторно."
                    else "Не удалось загрузить карточку. Попробуйте ещё раз.", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
internal fun TrainingFooter(session: Training, busy: Boolean, onNext: (Long) -> Unit, onClose: () -> Unit) {
    PaperFooter {
        when {
            session.completed -> PaperButton("Вернуться в словарь", onClose, Modifier.fillMaxWidth(), !busy, icon = R.drawable.ic_arrow)
            session.loadFailed -> PaperButton("Повторить загрузку", { onNext(session.questionToken) }, Modifier.fillMaxWidth(), !busy)
            session.feedback != null -> PaperButton("Продолжить", { onNext(session.questionToken) }, Modifier.fillMaxWidth(), !busy, icon = R.drawable.ic_arrow)
            else -> Muted("Пять минут для себя и своего английского.")
        }
    }
}

@Composable
private fun AnswerFeedback(session: Training, feedback: Feedback) {
    Surface(shape = RoundedCornerShape(11.dp), color = if (feedback.correct) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
        contentColor = if (feedback.correct) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("answer-feedback")) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (feedback.mastered) "Слово выучено. Отличная работа!" else if (feedback.correct) "Верно. Так держать!" else "Теперь знаем. Попробуем ещё.",
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(if (session.mode == TrainingMode.REVIEW) {
                if (feedback.correct) "Ответ засчитан." else "Карточка вернётся в конце занятия. Статус «Выучено» сохраняется."
            } else when {
                feedback.mastered -> "Карточка вышла из пула и доступна для повторения."
                feedback.correct -> "Ещё немного практики — и слово останется в памяти."
                else -> "Серия сброшена. Карточка остаётся в пуле."
            }, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Completion(session: Training) {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        PaperIcon(R.drawable.ic_sun, modifier = Modifier.size(85.dp), tint = MaterialTheme.colorScheme.primary)
        Eyebrow("Можно собой гордиться", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(if (session.mode == TrainingMode.LEARN) "Слова стали\nближе." else "Всё на своём\nместе.",
            style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
        Text(if (session.mode == TrainingMode.LEARN) "Вы закрепили все карточки этого пула. Возвращайтесь к ним в режиме повторения."
            else "Вы вспомнили все карточки этого занятия. Маленькое повторение — ещё один шаг вперёд.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Text("${session.total}", style = MaterialTheme.typography.headlineLarge.copy(fontSize = 56.sp, lineHeight = 62.sp), color = MaterialTheme.colorScheme.primary)
        Muted(if (session.mode == TrainingMode.LEARN) "карточек выучено" else "карточек повторено")
    }
}
