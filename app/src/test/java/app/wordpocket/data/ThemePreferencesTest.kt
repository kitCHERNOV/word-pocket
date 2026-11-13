package app.wordpocket.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThemePreferencesTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before fun prepare() { clearPreferences() }
    @After fun cleanup() { clearPreferences() }

    private fun clearPreferences() {
        context.getSharedPreferences("appearance", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun `no selection follows the system`() {
        assertNull(ThemePreferences(context).darkTheme)
    }

    @Test fun `new instance restores dark selection`() {
        ThemePreferences(context).setDarkTheme(true)
        assertEquals(true, ThemePreferences(context).darkTheme)
    }

    @Test fun `explicit light selection is preserved instead of system default`() {
        val preferences = ThemePreferences(context)
        preferences.setDarkTheme(true)
        preferences.setDarkTheme(false)
        assertEquals(false, ThemePreferences(context).darkTheme)
    }
}
