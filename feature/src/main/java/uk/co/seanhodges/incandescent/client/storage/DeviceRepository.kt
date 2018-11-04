package uk.co.seanhodges.incandescent.client.storage

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Database
import androidx.room.*
import androidx.room.Embedded
import java.io.Serializable
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import androidx.room.Room

const val DATABASE_NAME: String = "incandescent-device-register"

val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE room " + " ADD COLUMN chosen_count INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE device " + " ADD COLUMN chosen_count INTEGER NOT NULL DEFAULT 0")
        database.execSQL("CREATE INDEX idx_room_chosen_count ON  room(chosen_count)")
        database.execSQL("CREATE INDEX idx_device_chosen_count ON  device(chosen_count)")
    }
}

@Database(entities = [
    RoomEntity::class,
    DeviceEntity::class
], version = 2)
abstract class AppDatabase : RoomDatabase() {

    abstract fun roomDao(): RoomDao
    abstract fun deviceDao(): DeviceDao

    companion object {
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            if (INSTANCE == null) {
                INSTANCE = Room
                        .databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
                        .addMigrations(MIGRATION_1_2).build()
            }
            return INSTANCE!!
        }
    }
}

@Dao
interface RoomDao {

    @Query("SELECT * FROM room ORDER BY chosen_count DESC, id")
    fun loadAllWithDevices(): LiveData<List<RoomWithDevices>>

    @Query("SELECT count(id) FROM room")
    fun count(): Int

    @Query("UPDATE room SET chosen_count = chosen_count + 1 WHERE id = :id")
    fun incChosenCount(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertRoomAndDevices(room: RoomEntity, devices: List<DeviceEntity>)
}

@Dao
interface DeviceDao {

    @Query("UPDATE device SET chosen_count = chosen_count + 1 WHERE id = :id")
    fun incChosenCount(id: String)
}


@Entity(tableName = "room", indices = arrayOf(
        Index("chosen_count", name = "idx_room_chosen_count")
))
data class RoomEntity(

        @PrimaryKey
        var id: String,

        @ColumnInfo(name = "title")
        var title: String,

        @ColumnInfo(name = "chosen_count")
        var chosenCount: Int = 0
) : Serializable

@Entity(tableName = "device", indices = arrayOf(
        Index("chosen_count", name = "idx_device_chosen_count")
))
data class DeviceEntity(

        @PrimaryKey
        var id: String,

        @ColumnInfo(name = "title")
        var title: String,

        @ColumnInfo(name = "type")
        var type: String,

        @ColumnInfo(name = "power_command")
        var powerCommand: String?,

        @ColumnInfo(name = "dim_command")
        var dimCommand: String?,

        @ForeignKey(entity = RoomEntity::class,
                parentColumns = ["id"],
                childColumns = ["room_id"])
        @ColumnInfo(name = "room_id")
        var roomId: String,

        @ColumnInfo(name = "chosen_count")
        var chosenCount: Int = 0
) : Serializable

data class RoomWithDevices(

        @Embedded
    var room: RoomEntity? = null,

        @Relation(parentColumn = "id", entityColumn = "room_id", entity = DeviceEntity::class)
    var devices: List<DeviceEntity>? = null
) {

    // FIME: No way to sort a @Relation, sort devices in-memory
    fun getDevicesInOrder(): List<DeviceEntity> {
        return devices.orEmpty().sortedByDescending { it.chosenCount }
    }
}