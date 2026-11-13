package app.wordpocket.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.wordpocket.domain.DEFAULT_REQUIRED_STREAK
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "dictionaries")
data class DictionaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val poolSize: Int = 10,
    val poolTotal: Int = 0,
)

@Entity(
    tableName = "words",
    foreignKeys = [ForeignKey(entity = DictionaryEntity::class, parentColumns = ["id"], childColumns = ["dictionaryId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("dictionaryId"), Index(value = ["dictionaryId", "identity"], unique = true)],
)
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dictionaryId: Long,
    val word: String,
    val translation: String,
    val transcription: String = "",
    val identity: String,
    val streak: Int = 0,
    val learned: Boolean = false,
    @ColumnInfo(defaultValue = "0") val alreadyKnown: Boolean = false,
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,
    val requiredStreak: Int = DEFAULT_REQUIRED_STREAK,
)

@Entity(
    tableName = "pool",
    foreignKeys = [
        ForeignKey(entity = DictionaryEntity::class, parentColumns = ["id"], childColumns = ["dictionaryId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = WordEntity::class, parentColumns = ["id"], childColumns = ["wordId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("dictionaryId")],
)
data class PoolItem(@PrimaryKey val wordId: Long, val dictionaryId: Long, val turn: Long)

@Dao
interface WordDao {
    @Query("SELECT * FROM app_settings WHERE id = 1") fun observeSettings(): Flow<AppSettings?>
    @Query("SELECT * FROM app_settings WHERE id = 1") suspend fun settings(): AppSettings?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveSettings(settings: AppSettings)
    @Query("SELECT * FROM dictionaries ORDER BY id DESC") fun observeDictionaries(): Flow<List<DictionaryEntity>>
    @Query("SELECT * FROM words ORDER BY id") fun observeWords(): Flow<List<WordEntity>>
    @Query("SELECT * FROM pool ORDER BY turn, wordId") fun observePool(): Flow<List<PoolItem>>
    @Query("SELECT * FROM dictionaries WHERE id = :id") suspend fun dictionary(id: Long): DictionaryEntity?
    @Query("SELECT * FROM words WHERE dictionaryId = :dictionaryId ORDER BY id") suspend fun words(dictionaryId: Long): List<WordEntity>
    @Query("SELECT * FROM words WHERE id = :id") suspend fun word(id: Long): WordEntity?
    @Query("SELECT * FROM pool WHERE dictionaryId = :dictionaryId ORDER BY turn, wordId") suspend fun pool(dictionaryId: Long): List<PoolItem>
    @Insert suspend fun insertDictionary(dictionary: DictionaryEntity): Long
    @Update suspend fun updateDictionary(dictionary: DictionaryEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertWord(word: WordEntity): Long
    @Update suspend fun updateWord(word: WordEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun savePool(items: List<PoolItem>)
    @Query("DELETE FROM pool WHERE wordId = :wordId") suspend fun removeFromPool(wordId: Long)
    @Query("DELETE FROM words WHERE id = :id") suspend fun deleteWord(id: Long)
    @Query("DELETE FROM dictionaries WHERE id = :id") suspend fun deleteDictionary(id: Long)
}

@Database(entities = [DictionaryEntity::class, WordEntity::class, PoolItem::class, AppSettings::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): WordDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE words ADD COLUMN alreadyKnown INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE IF NOT EXISTS app_settings (id INTEGER NOT NULL, requiredStreak INTEGER NOT NULL, PRIMARY KEY(id))")
                db.execSQL("INSERT INTO app_settings (id, requiredStreak) VALUES (1, 1)")
            }
        }

        val INITIAL_SETTINGS = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL("INSERT INTO app_settings (id, requiredStreak) VALUES (1, $DEFAULT_REQUIRED_STREAK)")
            }
        }
    }
}
