package app.wordpocket.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.wordpocket.R
import app.wordpocket.data.*

@Composable
internal fun LibraryScreen(library: Library, busy: Boolean, onOpen: (Long) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns = if (maxWidth >= 600.dp) 2 else 1
        val metricColumns = if (maxWidth < 500.dp || LocalDensity.current.fontScale > 1.3f) 2 else 4
        Column(Modifier.fillMaxSize()) {
            LazyVerticalGrid(columns = GridCells.Fixed(columns), modifier = Modifier.weight(1f).testTag("library-grid"),
                contentPadding = PaddingValues(22.dp, 12.dp, 22.dp, 26.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(Modifier.padding(top = 10.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Eyebrow("Понемногу. Каждый день.")
                        Text("Слова остаются\nс вами.", style = MaterialTheme.typography.headlineLarge)
                        Muted("Собирайте важное. Учите в своём темпе.\nВозвращайтесь, чтобы помнить.")
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(Modifier.padding(bottom = 12.dp)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        val metrics = listOf(
                            library.dictionaries.size to "словарей", library.words.size to "карточек",
                            library.words.count { it.learned } to "выучено", library.words.count { it.alreadyKnown } to "уже знаю",
                        )
                        metrics.chunked(metricColumns).forEach { row ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                row.forEach { (count, label) -> Metric("$count", label, Modifier.weight(1f)) }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Ваши словари", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                            Muted("${library.dictionaries.size}", Modifier.padding(horizontal = 10.dp, vertical = 2.dp))
                        }
                    }
                }
                items(library.dictionaries, key = { it.id }) { dictionary ->
                    val words = library.words.filter { it.dictionaryId == dictionary.id }
                    val learned = words.count { it.learned }
                    val known = words.count { it.alreadyKnown }
                    Surface(onClick = { onOpen(dictionary.id) }, enabled = !busy, shape = RoundedCornerShape(15.dp),
                        color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(17.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Surface(shape = RoundedCornerShape(7.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                    Box(Modifier.size(42.dp, 46.dp), contentAlignment = Alignment.Center) {
                                        PaperIcon(R.drawable.ic_book, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(dictionary.name, style = MaterialTheme.typography.titleLarge)
                                    Muted("${words.size} карточек · $learned выучено · $known уже знаю" +
                                        if (library.pool.any { it.dictionaryId == dictionary.id }) " · в процессе" else "")
                                }
                                PaperIcon(R.drawable.ic_chevron, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            ThinProgress(if (words.isEmpty()) 0f else (learned + known).toFloat() / words.size, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
                if (library.dictionaries.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState("Первый словарь — ваш", "Добавьте слова из книги, фильма или просто из жизни.")
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PaperIcon(R.drawable.ic_leaf, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("Маленькая привычка.\nБольшой мир новых слов.", style = MaterialTheme.typography.titleMedium)
                            Muted("Пять минут для себя и своего английского.")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Metric(value: String, label: String, modifier: Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Start)
    }
}
