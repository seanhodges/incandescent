package uk.co.seanhodges.incandescent.client.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

const val DATABASE_NAME: String = "incandescent-device-register"

@Database(entities = [
    RoomEntity::class,
    DeviceEntity::class,
    SceneEntity::class,
    SceneActionEntity::class,
    FeaturesetHeatingEntity::class
], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun roomDao(): RoomDao
    abstract fun deviceDao(): DeviceDao
    abstract fun sceneDao(): SceneDao
    abstract fun deviceFeatureDao(): DeviceFeatureDao

    companion object {
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            if (INSTANCE == null) {
                INSTANCE = Room
                        .databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                        .addMigrations(MIGRATION_1_2)
                        .addMigrations(MIGRATION_2_3)
                        .addMigrations(MIGRATION_3_4)
                        .addMigrations(MIGRATION_4_5)
                        .build()
            }
            return INSTANCE!!
        }
    }
}

val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE room ADD COLUMN chosen_count INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE device ADD COLUMN chosen_count INTEGER NOT NULL DEFAULT 0")
        database.execSQL("CREATE INDEX idx_room_chosen_count ON room(chosen_count)")
        database.execSQL("CREATE INDEX idx_device_chosen_count ON device(chosen_count)")
    }
}

val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE device ADD COLUMN last_value_power INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE device ADD COLUMN last_value_dim INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // A funny thing happened for this migration; for some upgrading devices
        // Room had created empty scene tables, but for others these didn't exist.
        // For this migration we force the tables to be recreated
        database.execSQL("DROP TABLE IF EXISTS scene")
        database.execSQL(
        """
            CREATE TABLE scene(
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                chosen_count INTEGER NOT NULL)
            """)
        database.execSQL("DROP TABLE IF EXISTS scene_action")
        database.execSQL(
            """
            CREATE TABLE scene_action (
                scene_id INTEGER,
                id TEXT NOT NULL PRIMARY KEY,
                feature_value INTEGER NOT NULL)
            """)
        database.execSQL("CREATE INDEX IF NOT EXISTS idx_scene_chosen_count ON scene(chosen_count)")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_scene_title ON scene(title)")
    }
}

val MIGRATION_4_5: Migration = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE device ADD COLUMN power_usage_command TEXT")
        database.execSQL("ALTER TABLE device ADD COLUMN energy_consumption_command TEXT")
        database.execSQL("ALTER TABLE device ADD COLUMN hidden INTEGER NOT NULL DEFAULT 0")
    }
}