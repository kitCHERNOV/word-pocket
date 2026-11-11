@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package app.wordpocket.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.wordpocket.data.*
import app.wordpocket.domain.WordDraft

@Composable
fun WordPocketRoot(vm: AppViewModel) {
    val library by vm.library.collectAsStateWithLifecycle()
    val training by vm.training.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var dictionaryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var createDictionary by rememberSaveable { mutableStateOf(false) }
    var exitTraining by rememberSaveable { mutableStateOf(false) }
    var importTarget by rememberSaveable { mutableStateOf<Long?>(null) }
    var exportTarget by rememberSaveable { mutableStateOf<Long?>(null) }
    val snackbars = remember { SnackbarHostState() }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val target = importTarget
        if (uri != null && target != null) vm.importJson(target, uri)
        importTarget = null
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val target = exportTarget
        if (uri != null && target != null) vm.exportJson(target, uri)
        exportTarget = null
    }
    LaunchedEffect(vm) { vm.messages.collect { snackbars.showSnackbar(it) } }
    fun back() {
        if (!busy) {
            if (training != null && training?.completed != true) exitTraining = true
            else if (training != null) vm.closeTraining()
            else dictionaryId = null
        }
    }
    BackHandler(enabled = dictionaryId != null || training != null) { back() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbars) },
        topBar = {
            TopAppBar(
                title = { Text(if (dictionaryId == null && training == null) "WordPocket" else if (training != null) "Тренировка" else "Мой словарь", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (dictionaryId != null || training != null) TextButton(onClick = { back() }, enabled = !busy) { Text("‹ Назад") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).imePadding(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
                val session = training
                val dictionary = library.dictionaries.find { it.id == dictionaryId }
                when {
                    session != null -> TrainingScreen(session, busy, vm::setAnswerMode, vm::answer, vm::nextQuestion, vm::closeTraining)
                    dictionaryId == null -> LibraryScreen(library, busy, { dictionaryId = it }, { createDictionary = true })
                    dictionary != null -> key(dictionary.id) {
                        DictionaryScreen(
                            dictionary, library.words.filter { it.dictionaryId == dictionary.id },
                            library.pool.filter { it.dictionaryId == dictionary.id }, busy, vm,
                            onImport = { importTarget = dictionary.id; importLauncher.launch(arrayOf("application/json", "text/*", "application/octet-stream")) },
                            onExport = { exportTarget = dictionary.id; exportLauncher.launch("wordpocket-${dictionary.id}.json") },
                            onDeleted = { dictionaryId = null },
                        )
                    }
                    else -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }
    }
    if (createDictionary) DictionaryDialog(null, busy, { createDictionary = false }) { name, _ ->
        vm.createDictionary(name) { dictionaryId = it; createDictionary = false }
    }
    if (exitTraining) ConfirmDialog(
        "Закончить занятие?",
        if (training?.mode == TrainingMode.LEARN) "Пул и результаты ответов сохранены. Продолжить можно из этого словаря."
        else "Статусы выученных слов сохранятся. Следующее повторение начнётся заново.",
        "Выйти", busy, { exitTraining = false }, { exitTraining = false; vm.closeTraining() },
    )
}

@Composable
private fun LibraryScreen(library: Library, busy: Boolean, onOpen: (Long) -> Unit, onCreate: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Слова, которые\nнужны тебе", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text("Личные словари. Небольшие шаги каждый день.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (library.words.isNotEmpty()) item {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Metric("${library.dictionaries.size}", "словарей")
                    Metric("${library.words.size}", "карточек")
                    Metric("${library.words.count { it.learned }}", "выучено")
                }
            }
        }
        item {
            Button(onClick = onCreate, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp)) {
                Text("+ Создать словарь")
            }
        }
        if (library.dictionaries.isEmpty()) item {
            EmptyCard("Aa", "Начни со своего словаря", "Например, «Путешествия» или «Работа». Добавляй слова и фразы вручную либо загрузи JSON.")
        }
        items(library.dictionaries, key = { it.id }) { dictionary ->
            val words = library.words.filter { it.dictionaryId == dictionary.id }
            val learned = words.count { it.learned }
            Card(onClick = { if (!busy) onOpen(dictionary.id) }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        LetterTile(dictionary.name.take(1).uppercase())
                        Column(Modifier.weight(1f)) {
                            Text(dictionary.name, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${words.size} карточек · $learned выучено", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("›", fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    LinearProgressIndicator(progress = { if (words.isEmpty()) 0f else learned.toFloat() / words.size }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape))
                    if (library.pool.any { it.dictionaryId == dictionary.id }) Text("Есть незавершённый пул", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun DictionaryScreen(
    dictionary: DictionaryEntity, words: List<WordEntity>, pool: List<PoolItem>, busy: Boolean,
    vm: AppViewModel, onImport: () -> Unit, onExport: () -> Unit, onDeleted: () -> Unit,
) {
    var editSettings by rememberSaveable { mutableStateOf(false) }
    var deleteDictionary by rememberSaveable { mutableStateOf(false) }
    var showWordEditor by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val learned = words.count { it.learned }
    val filtered = words.filter {
        (filter == 0 || (filter == 1 && !it.learned) || (filter == 2 && it.learned)) &&
            (it.word.contains(search, ignoreCase = true) || it.translation.contains(search, ignoreCase = true))
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text(dictionary.name, style = MaterialTheme.typography.headlineLarge)
            Text("${words.size} карточек · $learned выучено", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        }
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (pool.isEmpty()) "Твой следующий шаг" else "Продолжим с того же места", style = MaterialTheme.typography.titleLarge)
                    Text(if (pool.isEmpty()) "Новый пул — до ${dictionary.poolSize} карточек. Три верных ответа подряд закрепят каждую."
                        else "В текущем пуле осталось ${pool.size} из ${dictionary.poolTotal} карточек.")
                    Button(onClick = { vm.startTraining(dictionary.id, TrainingMode.LEARN) }, enabled = !busy && words.any { !it.learned }, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) {
                        Text(if (pool.isEmpty()) "Учить новые" else "Продолжить изучение")
                    }
                    OutlinedButton(onClick = { vm.startTraining(dictionary.id, TrainingMode.REVIEW) }, enabled = !busy && learned > 0, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)) {
                        Text("Повторить выученные · $learned")
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { editingId = null; showWordEditor = true }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("+ Карточка") }
                OutlinedButton(onClick = onImport, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Импорт JSON") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { editSettings = true }, enabled = !busy) { Text("Настроить словарь") }
                TextButton(onClick = onExport, enabled = !busy && words.isNotEmpty()) { Text("Экспорт") }
            }
        }
        if (words.isEmpty()) item { EmptyCard("+", "Здесь появятся твои слова", "Создай первую карточку или выбери JSON-файл. Транскрипцию можно заполнить по желанию.") }
        else {
            item {
                OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("Найти слово или перевод") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Все", "Учу", "Выучено").forEachIndexed { index, label -> FilterChip(selected = filter == index, onClick = { filter = index }, label = { Text(label) }) }
                }
            }
            if (filtered.isEmpty()) item { Text("Карточек с такими условиями нет.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(filtered, key = { it.id }) { word ->
                Card(onClick = { if (!busy) { editingId = word.id; showWordEditor = true } }, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(word.word, style = MaterialTheme.typography.titleLarge)
                        if (word.transcription.isNotBlank()) Text(word.transcription, color = MaterialTheme.colorScheme.primary)
                        Text(word.translation, style = MaterialTheme.typography.bodyLarge)
                        Text(if (word.learned) "✓ Выучено" else if (pool.any { it.wordId == word.id }) "В пуле · ${word.streak}/3" else "Новое",
                            style = MaterialTheme.typography.labelMedium, color = if (word.learned) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        item { TextButton(onClick = { deleteDictionary = true }, enabled = !busy) { Text("Удалить словарь", color = MaterialTheme.colorScheme.error) } }
    }
    if (editSettings) DictionaryDialog(dictionary, busy, { editSettings = false }) { name, size ->
        vm.updateDictionary(dictionary.id, name, size) { editSettings = false }
    }
    if (showWordEditor) WordDialog(words.find { it.id == editingId }, busy, { showWordEditor = false },
        onSave = { draft -> vm.saveWord(dictionary.id, editingId, draft) { showWordEditor = false } },
        onDelete = editingId?.let { id -> { vm.deleteWord(id) { showWordEditor = false } } },
    )
    if (deleteDictionary) ConfirmDialog("Удалить словарь?", "Все карточки и прогресс словаря «${dictionary.name}» будут удалены с телефона.", "Удалить", busy,
        { deleteDictionary = false }, { vm.deleteDictionary(dictionary.id) { deleteDictionary = false; onDeleted() } })
}

@Composable
private fun TrainingScreen(session: Training, busy: Boolean, onMode: (AnswerMode) -> Unit, onAnswer: (String?) -> Unit, onNext: () -> Unit, onClose: () -> Unit) {
    var answer by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(session.question?.id, session.feedback) { if (session.feedback == null) answer = "" }
    val writing = session.answerMode == AnswerMode.WRITING || session.choices.isEmpty()
    val feedback = session.feedback
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(session.dictionaryName, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        if (session.completed) {
            Spacer(Modifier.height(24.dp))
            EmptyCard("✓", if (session.mode == TrainingMode.LEARN) "Пул пройден!" else "Повторение завершено!",
                if (session.mode == TrainingMode.LEARN) "Все ${session.total} карточек закреплены. Теперь они доступны для повторения в этом словаре."
                else "Все ${session.total} карточек получили правильный ответ в этом занятии.")
            Button(onClick = onClose, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Вернуться в словарь") }
            return@Column
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (session.mode == TrainingMode.LEARN) "Изучение" else "Повторение", style = MaterialTheme.typography.titleLarge)
            Text("${session.total - session.remaining} / ${session.total}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(progress = { if (session.total == 0) 0f else (session.total - session.remaining).toFloat() / session.total }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(selected = !writing, onClick = { onMode(AnswerMode.CHOICES) }, enabled = !busy && feedback == null && session.choices.isNotEmpty(), label = { Text("4 варианта") })
            FilterChip(selected = writing, onClick = { onMode(AnswerMode.WRITING) }, enabled = !busy && feedback == null, label = { Text("Написать") })
        }
        if (session.choices.isEmpty()) Text("Для четырёх вариантов нужны ещё три разных ответа из этого словаря. Пока используй ввод.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val word = session.question ?: return@Column
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("КАК ЭТО ПО-АНГЛИЙСКИ?", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(word.translation, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                if (feedback != null) {
                    HorizontalDivider()
                    Text(word.word, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                    if (word.transcription.isNotBlank()) Text(word.transcription, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
                if (session.mode == TrainingMode.LEARN) Text("Верных ответов подряд: ${feedback?.streak ?: word.streak}/3", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (feedback == null) {
            if (writing) {
                OutlinedTextField(value = answer, onValueChange = { answer = it }, modifier = Modifier.fillMaxWidth(), enabled = !busy,
                    label = { Text("Английское слово или фраза") }, shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (answer.isNotBlank() && !busy) onAnswer(answer) }), maxLines = 5)
                Button(onClick = { onAnswer(answer) }, enabled = !busy && answer.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Проверить") }
            } else {
                session.choices.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { choice ->
                            OutlinedButton(onClick = { onAnswer(choice) }, enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 76.dp), shape = RoundedCornerShape(18.dp), contentPadding = PaddingValues(14.dp)) {
                                Text(choice, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
            TextButton(onClick = { onAnswer(null) }, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Не знаю · Don’t know") }
        } else {
            Surface(shape = RoundedCornerShape(20.dp), color = if (feedback.correct) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (feedback.mastered) "✓ Карточка выучена" else if (feedback.correct) "Верно!" else "Запомним и попробуем ещё", fontWeight = FontWeight.Bold)
                    Text(if (session.mode == TrainingMode.LEARN) {
                        if (feedback.mastered) "Она вышла из пула и доступна для повторения."
                        else if (feedback.correct) "Слово появится снова для закрепления."
                        else "Серия сброшена. Карточка остаётся в пуле."
                    } else if (feedback.correct) "Ответ засчитан." else "Карточка вернётся в этом занятии. Статус «Выучено» сохранён.")
                }
            }
            Button(onClick = onNext, enabled = !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)) { Text("Дальше") }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun DictionaryDialog(dictionary: DictionaryEntity?, busy: Boolean, onDismiss: () -> Unit, onSave: (String, Int) -> Unit) {
    var name by rememberSaveable { mutableStateOf(dictionary?.name.orEmpty()) }
    var poolSize by rememberSaveable { mutableStateOf((dictionary?.poolSize ?: 10).toString()) }
    val valid = name.isNotBlank() && name.trim().length <= 80 && (poolSize.toIntOrNull() ?: 0) in 1..100
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text(if (dictionary == null) "Новый словарь" else "Настройки словаря") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Название") }, singleLine = true, enabled = !busy, supportingText = { Text("Например: Путешествия") })
                if (dictionary != null) {
                    OutlinedTextField(poolSize, { poolSize = it }, label = { Text("Размер пула") }, singleLine = true, enabled = !busy,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), supportingText = { Text("От 1 до 100 новых карточек") })
                    Text("Размер применяется к следующему пулу. Текущий пул сохраняется.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }, confirmButton = { TextButton(onClick = { onSave(name, poolSize.toInt()) }, enabled = !busy && valid) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Отмена") } })
}

@Composable
private fun WordDialog(word: WordEntity?, busy: Boolean, onDismiss: () -> Unit, onSave: (WordDraft) -> Unit, onDelete: (() -> Unit)?) {
    var english by rememberSaveable { mutableStateOf(word?.word.orEmpty()) }
    var translation by rememberSaveable { mutableStateOf(word?.translation.orEmpty()) }
    var transcription by rememberSaveable { mutableStateOf(word?.transcription.orEmpty()) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text(if (word == null) "Новая карточка" else "Редактировать карточку") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(english, { english = it }, label = { Text("Слово или фраза") }, enabled = !busy, maxLines = 4, supportingText = { Text("На английском · до 300 символов") })
                OutlinedTextField(transcription, { transcription = it }, label = { Text("Транскрипция") }, enabled = !busy, maxLines = 3, supportingText = { Text("Необязательно · например /ˈdʒɜːni/") })
                OutlinedTextField(translation, { translation = it }, label = { Text("Перевод") }, enabled = !busy, maxLines = 5)
                if (word != null) Text("Изменение слова или перевода сбросит его прогресс. Изменение транскрипции сохранит прогресс.", style = MaterialTheme.typography.bodySmall)
                if (onDelete != null) TextButton(onClick = { confirmDelete = true }, enabled = !busy) { Text("Удалить карточку", color = MaterialTheme.colorScheme.error) }
            }
        }, confirmButton = { TextButton(onClick = { onSave(WordDraft(english, translation, transcription)) }, enabled = !busy && english.isNotBlank() && translation.isNotBlank() && english.trim().length <= 300 && translation.trim().length <= 500 && transcription.trim().length <= 300) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Отмена") } })
    if (confirmDelete && onDelete != null) ConfirmDialog("Удалить карточку?", "Слово и его прогресс будут удалены из словаря.", "Удалить", busy, { confirmDelete = false }, onDelete)
}

@Composable
private fun ConfirmDialog(title: String, message: String, confirm: String, busy: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text(title) }, text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm, enabled = !busy) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Отмена") } })
}

@Composable
private fun LetterTile(letter: String) {
    Box(Modifier.size(50.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
        Text(letter, color = MaterialTheme.colorScheme.primary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Metric(value: String, label: String) {
    Column { Text(value, style = MaterialTheme.typography.headlineMedium); Text(label, style = MaterialTheme.typography.bodySmall) }
}

@Composable
private fun EmptyCard(symbol: String, title: String, description: String) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(26.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(72.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                Text(symbol, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}
