package app.wordpocket.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.wordpocket.data.DictionaryEntity
import app.wordpocket.data.Library
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class LibraryLayoutTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun showLibrary() {
        val library = Library(dictionaries = listOf(
            DictionaryEntity(id = 1, name = "Первый словарь"),
            DictionaryEntity(id = 2, name = "Второй словарь"),
        ))
        compose.setContent {
            WordPocketTheme {
                Surface(Modifier.fillMaxSize()) { LibraryScreen(library, busy = false, onOpen = {}) }
            }
        }
        compose.onNodeWithText("Первый словарь").assertIsDisplayed()
        compose.onNodeWithText("Второй словарь").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w600dp-h1000dp-mdpi")
    fun `library shows two columns from 600 dp`() {
        showLibrary()
        val first = compose.onNodeWithText("Первый словарь").fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithText("Второй словарь").fetchSemanticsNode().boundsInRoot
        assertEquals(first.top, second.top, 1f)
        assertTrue(second.left > first.right)
    }

    @Test
    @Config(qualifiers = "w599dp-h1000dp-mdpi")
    fun `library keeps one column below 600 dp`() {
        showLibrary()
        val first = compose.onNodeWithText("Первый словарь").fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithText("Второй словарь").fetchSemanticsNode().boundsInRoot
        assertEquals(first.left, second.left, 1f)
        assertTrue(second.top > first.bottom)
    }
}
