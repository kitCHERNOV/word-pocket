@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package app.wordpocket.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.wordpocket.R

@Composable
fun WordPocketRoot(vm: AppViewModel, darkTheme: Boolean, onToggleTheme: () -> Unit) {
    val library by vm.library.collectAsStateWithLifecycle()
    val training by vm.training.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    var dictionaryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var createDictionary by rememberSaveable { mutableStateOf(false) }
    var editSettings by rememberSaveable { mutableStateOf(false) }
    var learningSettings by rememberSaveable { mutableStateOf(false) }
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
            if (dictionaryId == null && training == null) {
                LibraryHeader(darkTheme, onToggleTheme, !busy && settings != null, !busy,
                    onSettings = { learningSettings = true }, onCreate = { createDictionary = true })
            } else {
                Row(Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                    !busy && library.words.any { it.dictionaryId == dictionary.id && !it.learned && !it.alreadyKnown }, icon = R.drawable.ic_arrow)
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
                        session != null -> TrainingScreen(session, busy, vm::setAnswerMode, vm::answer, vm::markQuestionAlreadyKnown)
                        dictionaryId == null -> LibraryScreen(library, busy, { dictionaryId = it })
                        dictionary != null -> key(dictionary.id) {
                            DictionaryScreen(dictionary, library.words.filter { it.dictionaryId == dictionary.id },
                                library.pool.filter { it.dictionaryId == dictionary.id }, busy, vm, settings?.requiredStreak ?: 1,
                                onImport = { importTarget = dictionary.id; importLauncher.launch(arrayOf("application/json", "text/*", "application/octet-stream")) },
                                onExport = { exportTarget = dictionary.id; exportLauncher.launch("kitlang-${dictionary.id}.json") })
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
    if (learningSettings) settings?.let { current ->
        LearningSettingsDialog(current, busy, { learningSettings = false }) { requiredStreak ->
            vm.updateLearningSettings(requiredStreak) { learningSettings = false }
        }
    }
    if (exitTraining) ConfirmDialog("Закончить занятие?",
        if (training?.mode == TrainingMode.LEARN) "Ответы и текущий пул сохранены. Продолжите, когда будет удобно."
        else "Выученные слова сохранят свой статус. Следующее повторение начнётся заново.",
        "Выйти", busy, { exitTraining = false }, { exitTraining = false; vm.closeTraining() })
}

@Composable
private fun LibraryHeader(
    darkTheme: Boolean, onToggleTheme: () -> Unit, settingsEnabled: Boolean, createEnabled: Boolean,
    onSettings: () -> Unit, onCreate: () -> Unit,
) {
    BoxWithConstraints(Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(horizontal = 22.dp, vertical = 8.dp)) {
        val brand: @Composable () -> Unit = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Image(painterResource(R.drawable.ic_kitlang), contentDescription = null, modifier = Modifier.size(44.dp))
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
            }
        }
        val actions: @Composable () -> Unit = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleTheme, modifier = Modifier.size(48.dp)) {
                    PaperIcon(if (darkTheme) R.drawable.ic_sun else R.drawable.ic_moon,
                        description = if (darkTheme) "Включить светлую тему" else "Включить тёмную тему",
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                }
                PaperIconButton(R.drawable.ic_settings, "Настройки обучения", settingsEnabled, onSettings)
                PaperIconButton(R.drawable.ic_plus, "Создать словарь", createEnabled, onCreate)
            }
        }
        if (maxWidth < 340.dp || LocalDensity.current.fontScale > 1.3f) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                brand()
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) { actions() }
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                brand()
                actions()
            }
        }
    }
}
