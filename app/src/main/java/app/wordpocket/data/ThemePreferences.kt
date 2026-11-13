package app.wordpocket.data

import android.content.Context
import androidx.core.content.edit

class ThemePreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("appearance", Context.MODE_PRIVATE)

    val darkTheme: Boolean?
        get() = if (preferences.contains(DARK_THEME)) preferences.getBoolean(DARK_THEME, false) else null

    fun setDarkTheme(darkTheme: Boolean) {
        preferences.edit { putBoolean(DARK_THEME, darkTheme) }
    }

    private companion object {
        const val DARK_THEME = "dark_theme"
    }
}
