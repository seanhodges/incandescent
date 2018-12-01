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
    SceneActionEntity::class
], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun roomDao(): RoomDao
    abstract fun deviceDao(): DeviceDao
    abstract fun sceneDao(): SceneDao

    companion object {
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            if (INSTANCE == null) {
                INSTANCE = Room
                        .databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                        .addMigrations(MIGRATION_1_2)
                        .addMigrations(MIGRATION_2_3)
                        .addMigrations(MIGRATION_3_4)
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
        database.execSQL("ALTER TABLE scene ADD COLUMN id INTEGER NOT NULL")
        database.execSQL("ALTER TABLE scene ADD COLUMN title TEXT NOT NULL")
        database.execSQL("ALTER TABLE scene ADD COLUMN chosen_count INTEGER NOT NULL DEFAULT 0")
        database.execSQL("CREATE INDEX idx_scene_chosen_count ON scene(chosen_count)")
        database.execSQL("CREATE INDEX idx_scene_title ON scene(title)")
    }
}