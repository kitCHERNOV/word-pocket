package app.wordpocket.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.wordpocket.data.*
import app.wordpocket.domain.WordDraft

@Composable
private fun PaperDialog(title: String, intro: String, busy: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column(Modifier.heightIn(max = 640.dp).verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Muted(intro)
                content()
            }
        }
    }
}

@Composable
internal fun DictionaryDialog(
    dictionary: DictionaryEntity?, busy: Boolean, onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null, onSave: (String, Int) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(dictionary?.name.orEmpty()) }
    var poolSize by rememberSaveable { mutableStateOf((dictionary?.poolSize ?: 10).toString()) }
    val valid = name.isNotBlank() && name.trim().length <= 80 && (poolSize.toIntOrNull() ?: 0) in 1..100
    PaperDialog(if (dictionary == null) "Новый словарь" else "Настроим словарь",
        if (dictionary == null) "Соберите слова вокруг того, что вам интересно." else "Небольшие настройки для вашего ритма.", busy, onDismiss) {
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), enabled = !busy, label = { Text("Название") },
            singleLine = true, shape = RoundedCornerShape(9.dp), supportingText = { Text("От 1 до 80 символов") })
        OutlinedTextField(poolSize, { poolSize = it }, Modifier.fillMaxWidth(), enabled = !busy, label = { Text("Карточек в одном пуле") },
            singleLine = true, shape = RoundedCornerShape(9.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            supportingText = { Text("От 1 до 100") })
        if (dictionary != null) Muted("Размер применяется к следующему пулу. Текущий пул сохраняется.")
        DialogActions {
            PaperButton("Отмена", onDismiss, Modifier.weight(1f), !busy, primary = false)
            PaperButton(if (dictionary == null) "Создать" else "Сохранить", { onSave(name, poolSize.toInt()) }, Modifier.weight(1f), !busy && valid)
        }
        if (onDelete != null) TextButton(onClick = onDelete, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Удалить словарь", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
internal fun DictionarySettings(dictionary: DictionaryEntity, busy: Boolean, vm: AppViewModel, onDismiss: () -> Unit, onDeleted: () -> Unit) {
    var deleting by rememberSaveable { mutableStateOf(false) }
    if (deleting) ConfirmDialog("Удалить словарь?", "Все карточки и прогресс словаря «${dictionary.name}» будут удалены с телефона.",
        "Удалить", busy, { deleting = false }, { vm.deleteDictionary(dictionary.id, onDeleted) }, dangerous = true)
    else DictionaryDialog(dictionary, busy, onDismiss, onDelete = { deleting = true }) { name, size ->
        vm.updateDictionary(dictionary.id, name, size, onDismiss)
    }
}

@Composable
internal fun WordDialog(word: WordEntity?, busy: Boolean, onDismiss: () -> Unit, onSave: (WordDraft) -> Unit, onDelete: (() -> Unit)?) {
    var english by rememberSaveable { mutableStateOf(word?.word.orEmpty()) }
    var translation by rememberSaveable { mutableStateOf(word?.translation.orEmpty()) }
    var transcription by rememberSaveable { mutableStateOf(word?.transcription.orEmpty()) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    if (deleting && onDelete != null) {
        ConfirmDialog("Удалить карточку?", "Слово и его прогресс будут удалены из этого словаря.", "Удалить", busy, { deleting = false }, onDelete, dangerous = true)
        return
    }
    PaperDialog(if (word == null) "Новая карточка" else "Ваша карточка", "Одно слово или целая фраза — всё, что хочется запомнить.", busy, onDismiss) {
        OutlinedTextField(english, { english = it }, Modifier.fillMaxWidth(), enabled = !busy, label = { Text("Слово или фраза") },
            maxLines = 4, shape = RoundedCornerShape(9.dp), supportingText = { Text("На английском · до 300 символов") })
        OutlinedTextField(transcription, { transcription = it }, Modifier.fillMaxWidth(), enabled = !busy, label = { Text("Транскрипция") },
            maxLines = 3, shape = RoundedCornerShape(9.dp), supportingText = { Text("Необязательно · до 300 символов") })
        OutlinedTextField(translation, { translation = it }, Modifier.fillMaxWidth(), enabled = !busy, label = { Text("Перевод") },
            maxLines = 5, shape = RoundedCornerShape(9.dp), supportingText = { Text("До 500 символов") })
        if (word != null) Muted("Изменение слова или перевода сбросит его прогресс. Транскрипцию можно менять без сброса.")
        DialogActions {
            PaperButton("Отмена", onDismiss, Modifier.weight(1f), !busy, primary = false)
            PaperButton("Сохранить", { onSave(WordDraft(english, translation, transcription)) }, Modifier.weight(1f),
                !busy && english.isNotBlank() && translation.isNotBlank() && english.trim().length <= 300 && translation.trim().length <= 500 && transcription.trim().length <= 300)
        }
        if (onDelete != null) TextButton(onClick = { deleting = true }, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Удалить карточку", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
internal fun ConfirmDialog(
    title: String, message: String, confirm: String, busy: Boolean,
    onDismiss: () -> Unit, onConfirm: () -> Unit, dangerous: Boolean = false,
) {
    PaperDialog(title, message, busy, onDismiss) {
        DialogActions {
            PaperButton("Отмена", onDismiss, Modifier.weight(1f), !busy, primary = false)
            PaperButton(confirm, onConfirm, Modifier.weight(1f), !busy, dangerous = dangerous)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DialogActions(content: @Composable RowScope.() -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        FlowRow(Modifier.fillMaxWidth(), maxItemsInEachRow = if (maxWidth < 240.dp * fontScale) 1 else 2,
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}
