package app.wordpocket.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.wordpocket.R
import app.wordpocket.data.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DictionaryScreen(
    dictionary: DictionaryEntity, words: List<WordEntity>, pool: List<PoolItem>, busy: Boolean,
    vm: AppViewModel, requiredStreak: Int, onImport: () -> Unit, onExport: () -> Unit,
) {
    var showWordEditor by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val learned = words.count { it.learned }
    val known = words.count { it.alreadyKnown }
    val poolIds = pool.map { it.wordId }.toSet()
    val filtered = words.filter {
        (filter == 0 || (filter == 1 && !it.learned && !it.alreadyKnown) ||
            (filter == 2 && it.learned) || (filter == 3 && it.alreadyKnown)) &&
            (it.word.contains(search, ignoreCase = true) || it.translation.contains(search, ignoreCase = true))
    }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f).testTag("dictionary-list"), contentPadding = PaddingValues(22.dp, 12.dp, 22.dp, 26.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Column(Modifier.padding(bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Eyebrow("Личная коллекция", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(dictionary.name, style = MaterialTheme.typography.headlineMedium)
                    Muted("${words.size} карточек · $learned выучено · $known уже знаю")
                }
            }
            item {
                Surface(shape = RoundedCornerShape(15.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Eyebrow(if (pool.isEmpty()) "Один небольшой шаг" else "Продолжим с того же места", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(if (pool.isNotEmpty()) "${pool.size} карточек ещё в пуле"
                            else if (words.isEmpty()) "Добавьте первые карточки"
                            else if (words.any { !it.learned && !it.alreadyKnown }) "Новые слова ждут вас"
                            else "Все карточки уже знакомы", style = MaterialTheme.typography.headlineSmall)
                        Text(if (pool.isEmpty()) "Порция до ${dictionary.poolSize} карточек. Правильных ответов подряд для освоения: $requiredStreak."
                            else "Всего в занятии ${dictionary.poolTotal}. Ваши ответы сохранены.", style = MaterialTheme.typography.bodyMedium)
                        PaperButton("Повторить выученные", { vm.startTraining(dictionary.id, TrainingMode.REVIEW) },
                            Modifier.fillMaxWidth(), !busy && learned > 0, primary = false, icon = R.drawable.ic_repeat)
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Карточки", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    PaperIconButton(R.drawable.ic_plus, "Добавить карточку", !busy) { editingId = null; showWordEditor = true }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaperButton("Импорт JSON", onImport, Modifier.weight(1f), !busy, primary = false, icon = R.drawable.ic_upload)
                    PaperButton("Экспорт", onExport, Modifier.weight(1f), !busy && words.isNotEmpty(), primary = false, icon = R.drawable.ic_download)
                }
            }
            if (words.isEmpty()) item { EmptyState("Место для первых слов", "Добавьте карточку вручную или импортируйте JSON.") }
            else {
                item {
                    OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), enabled = !busy, singleLine = true,
                        label = { Text("Найти слово или перевод") }, leadingIcon = { PaperIcon(R.drawable.ic_search) }, shape = RoundedCornerShape(10.dp))
                    FlowRow(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Все", "Учу", "Выучено", "Уже знаю").forEachIndexed { index, label ->
                            FilterChip(selected = filter == index, onClick = { filter = index }, label = { Text(label) },
                                modifier = Modifier.heightIn(min = 48.dp).testTag("word-filter-$index"), shape = RoundedCornerShape(7.dp), border = null,
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant))
                        }
                    }
                }
                if (filtered.isEmpty()) item { EmptyState("Ничего не найдено", "Попробуйте другой запрос или фильтр.") }
                items(filtered, key = { it.id }) { word ->
                    Column {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(Modifier.fillMaxWidth().clickable(enabled = !busy, role = Role.Button) { editingId = word.id; showWordEditor = true }
                            .padding(vertical = 15.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(word.word, style = MaterialTheme.typography.titleLarge)
                                if (word.transcription.isNotBlank()) Muted(word.transcription)
                                Muted(word.translation)
                            }
                            Column(Modifier.widthIn(max = 90.dp), horizontalAlignment = Alignment.End) {
                                when {
                                    word.alreadyKnown -> Muted("Уже знаю")
                                    word.learned -> {
                                        PaperIcon(R.drawable.ic_check, tint = MaterialTheme.colorScheme.secondary)
                                        Text("Выучено", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                    }
                                    word.id in poolIds -> Muted("Учу")
                                    else -> Muted("Новое")
                                }
                            }
                        }
                        if (word.id in poolIds && !word.alreadyKnown && !word.learned) {
                            StreakProgress(word.streak, requiredStreak)
                        }
                        TextButton(onClick = {
                            if (word.alreadyKnown) vm.returnToLearning(word.id) else vm.markAlreadyKnown(word.id)
                        }, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp).testTag("known-${word.id}")) {
                            Text(if (word.alreadyKnown) "Вернуть в изучение" else "Already know")
                        }
                    }
                }
            }
        }
    }
    if (showWordEditor) WordDialog(words.find { it.id == editingId }, busy, { showWordEditor = false },
        onSave = { draft -> vm.saveWord(dictionary.id, editingId, draft) { showWordEditor = false } },
        onDelete = editingId?.let { id -> { vm.deleteWord(id) { showWordEditor = false } } })
}
