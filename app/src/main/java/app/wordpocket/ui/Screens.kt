@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package app.wordpocket.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.wordpocket.R

@Composable
fun WordPocketRoot(vm: AppViewModel) {
    val library by vm.library.collectAsStateWithLifecycle()
    val training by vm.training.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var dictionaryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var createDictionary by rememberSaveable { mutableStateOf(false) }
    var editSettings by rememberSaveable { mutableStateOf(false) }
    var exitTraining by rememberSaveable { mutableStateOf(false) }
    var importTarget by rememberSaveable { mutableStateOf<Long?>(null) }
    var exportTarget by rememberSaveable { mutableStateOf<Long?>(null) }
    val dictionary = library.dictionaries.find { it.id == dictionaryId }
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
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top))) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (dictionaryId == null && training == null) {
                    PaperIcon(R.drawable.ic_sun, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Text("WordPocket", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    PaperIconButton(R.drawable.ic_plus, "Создать словарь", !busy) { createDictionary = true }
                } else {
                    PaperIconButton(R.drawable.ic_back, "Назад", !busy) { back() }
                    Text(training?.dictionaryName ?: "Мой словарь", modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                    if (training == null && dictionary != null) PaperIconButton(R.drawable.ic_settings, "Настроить словарь", !busy) { editSettings = true }
                    else Spacer(Modifier.size(48.dp))
                }
            }
        }
        Scaffold(modifier = Modifier.weight(1f).imePadding().navigationBarsPadding(),
            contentWindowInsets = WindowInsets(0, 0, 0, 0), containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(Modifier.widthIn(max = if (training == null) 720.dp else 470.dp).fillMaxWidth()) {
                        val session = training
                        when {
                            session != null -> TrainingFooter(session, busy, vm::nextQuestion, vm::closeTraining)
                            dictionaryId == null -> PaperFooter {
                                PaperButton("Новый словарь", { createDictionary = true }, Modifier.fillMaxWidth(), !busy, icon = R.drawable.ic_plus)
                            }
                            dictionary != null -> PaperFooter {
                                val hasPool = library.pool.any { it.dictionaryId == dictionary.id }
                                PaperButton(if (hasPool) "Продолжить изучение" else "Учить новые слова",
                                    { vm.startTraining(dictionary.id, TrainingMode.LEARN) }, Modifier.fillMaxWidth(),
                                    !busy && library.words.any { it.dictionaryId == dictionary.id && !it.learned }, icon = R.drawable.ic_arrow)
                            }
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbars) }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = if (training == null) 720.dp else 470.dp).fillMaxSize()) {
                    val session = training
                    when {
                        session != null -> TrainingScreen(session, busy, vm::setAnswerMode, vm::answer)
                        dictionaryId == null -> LibraryScreen(library, busy, { dictionaryId = it })
                        dictionary != null -> key(dictionary.id) {
                            DictionaryScreen(dictionary, library.words.filter { it.dictionaryId == dictionary.id },
                                library.pool.filter { it.dictionaryId == dictionary.id }, busy, vm,
                                onImport = { importTarget = dictionary.id; importLauncher.launch(arrayOf("application/json", "text/*", "application/octet-stream")) },
                                onExport = { exportTarget = dictionary.id; exportLauncher.launch("wordpocket-${dictionary.id}.json") })
                        }
                        else -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    }
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }
    }
    if (createDictionary) DictionaryDialog(null, busy, { createDictionary = false }) { name, size ->
        vm.createDictionary(name, size) { dictionaryId = it; createDictionary = false }
    }
    if (editSettings && dictionary != null) DictionarySettings(dictionary, busy, vm,
        onDismiss = { editSettings = false }, onDeleted = { editSettings = false; dictionaryId = null })
    if (exitTraining) ConfirmDialog("Закончить занятие?",
        if (training?.mode == TrainingMode.LEARN) "Ответы и текущий пул сохранены. Продолжите, когда будет удобно."
        else "Выученные слова сохранят свой статус. Следующее повторение начнётся заново.",
        "Выйти", busy, { exitTraining = false }, { exitTraining = false; vm.closeTraining() })
}
