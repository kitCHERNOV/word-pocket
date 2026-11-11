package app.wordpocket.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF6558D9), onPrimary = Color.White,
    primaryContainer = Color(0xFFECE9FF), onPrimaryContainer = Color(0xFF393076),
    secondary = Color(0xFF56866C), secondaryContainer = Color(0xFFE1F2E8), onSecondaryContainer = Color(0xFF214C36),
    background = Color(0xFFF7F8FC), surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF0EFF7), onSurface = Color(0xFF242337),
    onSurfaceVariant = Color(0xFF76748B), outlineVariant = Color(0xFFE6E4EF),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFBEB3FF), primaryContainer = Color(0xFF403565),
    secondaryContainer = Color(0xFF294D3A), onSecondaryContainer = Color(0xFFB9F0CE),
    background = Color(0xFF15141C), surface = Color(0xFF201F2A), surfaceVariant = Color(0xFF2C2A3A),
)

@Composable
fun WordPocketTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = Typography(
            headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp),
            headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 33.sp),
            titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 28.sp),
            bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 25.sp),
        ),
        content = content,
    )
}
