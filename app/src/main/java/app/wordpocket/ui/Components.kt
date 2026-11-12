package app.wordpocket.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun PaperIcon(@DrawableRes icon: Int, description: String? = null, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    Icon(painterResource(icon), description, modifier.size(21.dp), tint = tint)
}

@Composable
internal fun PaperIconButton(@DrawableRes icon: Int, description: String, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedIconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(48.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        PaperIcon(icon, description)
    }
}

@Composable
internal fun PaperButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    primary: Boolean = true, @DrawableRes icon: Int? = null, dangerous: Boolean = false,
) {
    val content: @Composable RowScope.() -> Unit = {
        if (icon != null) { PaperIcon(icon); Spacer(Modifier.width(8.dp)) }
        Text(text, textAlign = TextAlign.Center)
    }
    if (primary) Button(onClick, modifier.heightIn(min = 48.dp), enabled = enabled,
        colors = if (dangerous) ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError,
        ) else ButtonDefaults.buttonColors(),
        shape = RoundedCornerShape(11.dp), contentPadding = PaddingValues(16.dp, 12.dp), content = content)
    else OutlinedButton(onClick, modifier.heightIn(min = 48.dp), enabled = enabled,
        shape = RoundedCornerShape(11.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
        contentPadding = PaddingValues(12.dp), content = content)
}

@Composable
internal fun PaperFooter(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally, content = content)
    }
}

@Composable
internal fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Text(text.uppercase(), modifier, style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold), color = color)
}

@Composable
internal fun Muted(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun StreakDots(streak: Int) {
    Row(Modifier.semantics { contentDescription = "$streak из 3 правильных ответов" }, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(3) { index ->
            Box(Modifier.size(6.dp).clip(CircleShape).background(
                if (index < streak) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))
        }
    }
}

@Composable
internal fun ThinProgress(progress: Float, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
        color = color, trackColor = MaterialTheme.colorScheme.surfaceVariant, gapSize = 0.dp, drawStopIndicator = {})
}

@Composable
internal fun EmptyState(title: String, description: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Aa", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
