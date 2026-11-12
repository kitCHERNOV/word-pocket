package app.wordpocket.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFFC36B4F), onPrimary = Color(0xFF1E100A),
    primaryContainer = Color(0xFFF2E7DC), onPrimaryContainer = Color(0xFF77513C),
    secondary = Color(0xFF537052), onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDF2E8), onSecondaryContainer = Color(0xFF537052),
    tertiary = Color(0xFF929B7A),
    background = Color(0xFFF8F6F1), onBackground = Color(0xFF30312B),
    surface = Color(0xFFFFFEFA), onSurface = Color(0xFF30312B),
    surfaceVariant = Color(0xFFEEECE5), onSurfaceVariant = Color(0xFF777970),
    surfaceContainer = Color(0xFFF2E7DC), surfaceContainerHigh = Color(0xFFEEECE5),
    outline = Color(0xFF777970), outlineVariant = Color(0xFFE6E3DA),
    error = Color(0xFFA54439), onError = Color.White,
    errorContainer = Color(0xFFFAEEE9), onErrorContainer = Color(0xFFA54439),
    surfaceTint = Color.Transparent,
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFDF987C), onPrimary = Color(0xFF302018),
    primaryContainer = Color(0xFF3C3028), onPrimaryContainer = Color(0xFFE4B79E),
    secondary = Color(0xFFB0C4A2), onSecondary = Color(0xFF243021),
    secondaryContainer = Color(0xFF303B2C), onSecondaryContainer = Color(0xFFB0C4A2),
    tertiary = Color(0xFFB0C4A2),
    background = Color(0xFF211F1B), onBackground = Color(0xFFEEE9DF),
    surface = Color(0xFF2B2823), onSurface = Color(0xFFEEE9DF),
    surfaceVariant = Color(0xFF36322C), onSurfaceVariant = Color(0xFFB8B1A5),
    surfaceContainer = Color(0xFF3C3028), surfaceContainerHigh = Color(0xFF36322C),
    outline = Color(0xFFB8B1A5), outlineVariant = Color(0xFF484238),
    error = Color(0xFFE7A59A), onError = Color(0xFF38201B),
    errorContainer = Color(0xFF432C28), onErrorContainer = Color(0xFFE7A59A),
    surfaceTint = Color.Transparent,
)

@Composable
fun WordPocketTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = Shapes(
            small = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
            extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        ),
        typography = Typography(
            headlineLarge = serif(39, 44, -1.2f),
            headlineMedium = serif(36, 43, -0.6f),
            headlineSmall = serif(28, 34, -0.6f),
            titleLarge = serif(22, 28, -0.4f),
            titleMedium = serif(19, 26),
            bodyLarge = sans(15, 23),
            bodyMedium = sans(13, 21),
            bodySmall = sans(12, 19),
            labelLarge = sans(14, 20, FontWeight.Medium),
            labelMedium = sans(11, 17),
            labelSmall = sans(10, 16),
        ), content = content,
    )
}

private fun serif(size: Int, line: Int, spacing: Float = 0f) = TextStyle(
    fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal,
    fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing.sp,
)
private fun sans(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = FontFamily.SansSerif, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp,
)
