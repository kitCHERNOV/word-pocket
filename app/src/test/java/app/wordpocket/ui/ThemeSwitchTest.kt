package app.wordpocket.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.core.view.WindowCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.wordpocket.MainActivity
import app.wordpocket.data.ThemePreferences
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class ThemeSwitchTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Before fun prepare() { clearPreferences() }
    @After fun cleanup() { clearPreferences() }

    private fun clearPreferences() {
        context.getSharedPreferences("appearance", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    @Config(qualifiers = "w430dp-h880dp-notnight-mdpi")
    fun `button switches both ways and updates system bars`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            assertTheme(scenario, dark = false)
            assertNull(ThemePreferences(context).darkTheme)
            screenshot(scenario, "library-light")
            compose.onNodeWithContentDescription("Включить тёмную тему").performClick()
            assertTheme(scenario, dark = true)
            assertEquals(true, ThemePreferences(context).darkTheme)
            screenshot(scenario, "library-dark")
            compose.onNodeWithContentDescription("Включить светлую тему").performClick()
            assertTheme(scenario, dark = false)
            assertEquals(false, ThemePreferences(context).darkTheme)
        }
    }

    @Test
    @Config(qualifiers = "w430dp-h880dp-night-mdpi")
    fun `system dark theme applies until manual light selection and survives recreation and relaunch`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            assertTheme(scenario, dark = true)
            assertNull(ThemePreferences(context).darkTheme)
            compose.onNodeWithContentDescription("Включить светлую тему").performClick()
            assertTheme(scenario, dark = false)
            assertEquals(false, ThemePreferences(context).darkTheme)
            scenario.recreate()
            assertTheme(scenario, dark = false)
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            assertTheme(scenario, dark = false)
            assertEquals(false, ThemePreferences(context).darkTheme)
        }
    }

    @Test
    @Config(qualifiers = "w430dp-h880dp-notnight-mdpi")
    fun `saved dark theme overrides light system on launch`() {
        ThemePreferences(context).setDarkTheme(true)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            assertTheme(scenario, dark = true)
        }
    }

    private fun assertTheme(scenario: ActivityScenario<MainActivity>, dark: Boolean) {
        compose.waitForIdle()
        compose.onNodeWithContentDescription(if (dark) "Включить светлую тему" else "Включить тёмную тему").assertIsDisplayed()
        compose.waitForIdle()
        scenario.onActivity { activity ->
            val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            assertEquals(!dark, controller.isAppearanceLightStatusBars)
            assertEquals(!dark, controller.isAppearanceLightNavigationBars)
        }
    }

    private fun screenshot(scenario: ActivityScenario<MainActivity>, name: String) {
        val directory = System.getProperty("wordpocket.screenshotDir")?.let { File(it, "theme-switch") } ?: return
        directory.mkdirs()
        val bounds = compose.onRoot().fetchSemanticsNode().boundsInWindow
        lateinit var bitmap: Bitmap
        scenario.onActivity { activity ->
            bitmap = Bitmap.createBitmap(bounds.width.toInt(), bounds.height.toInt(), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.translate(-bounds.left, -bounds.top)
            activity.window.decorView.draw(canvas)
        }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
