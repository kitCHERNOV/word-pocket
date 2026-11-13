package app.wordpocket

import android.app.Application
import androidx.room.Room
import app.wordpocket.data.AppDatabase
import app.wordpocket.data.WordRepository

class WordPocketApp : Application() {
    val repository: WordRepository by lazy {
        WordRepository(Room.databaseBuilder(this, AppDatabase::class.java, "word-pocket.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .addCallback(AppDatabase.INITIAL_SETTINGS)
            .build())
    }
}
