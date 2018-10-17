package uk.co.seanhodges.incandescent.client.selection

import android.content.Context
import androidx.room.Database
import androidx.room.*
import java.lang.ref.WeakReference
import androidx.room.Embedded
import java.io.Serializable

class DeviceRepository(ctx: Context) {

    private var db: AppDatabase
    private val ctxRef: WeakReference<Context> = WeakReference(ctx)

    private val DATABASE_NAME: String = "incandescent-device-register"

    init {
//        db = Room.databaseBuilder(ctx.get()!!,
//                AppDatabase::class.java, DATABASE_NAME).build()
        db = Room.inMemoryDatabaseBuilder(ctxRef.get()!!, AppDatabase::class.java).build()
    }

    fun buildTestDb() {
        db.roomDao().insertRoomAndDevices(RoomEntity("1", "Living room"), listOf(
                DeviceEntity(
                        "1",
                        "Main light",
                        "light",
                        FEATURE_LIVING_ROOM_POWER_ID,
                        FEATURE_LIVING_ROOM_DIM_ID,
                        "1"
                )
        ))
        db.roomDao().insertRoomAndDevices(RoomEntity("2", "Bedroom"), listOf(
                DeviceEntity(
                        "2",
                        "Main light",
                        "light",
                        FEATURE_BEDROOM_POWER_ID,
                        FEATURE_BEDROOM_DIM_ID,
                        "2"
                )
        ))
    }

    fun getAllRooms(): List<RoomWithDevices> {
        return db.roomDao().loadAllWithDevices()
    }

    companion object {
        private val FEATURE_LIVING_ROOM_POWER_ID = "5b8aa9b4d36c330fd5b4e100-22-3157332334+1"
        private val FEATURE_LIVING_ROOM_DIM_ID = "5b8aa9b4d36c330fd5b4e100-23-3157332334+1"

        private val FEATURE_BEDROOM_POWER_ID = "5b8aa9b4d36c330fd5b4e100-46-3157332334+1"
        private val FEATURE_BEDROOM_DIM_ID = "5b8aa9b4d36c330fd5b4e100-47-3157332334+1"
    }
}

@Database(entities = [
    RoomEntity::class,
    DeviceEntity::class
], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun roomDao(): RoomDao
}

@Dao
interface RoomDao {

    @Query("SELECT * FROM room")
    fun loadAllWithDevices(): List<RoomWithDevices>

    @Insert
    fun insertRoomAndDevices(user: RoomEntity, devices: List<DeviceEntity>)
}


@Entity(tableName = "room")
data class RoomEntity(

        @PrimaryKey
        var id: String,

        @ColumnInfo(name = "title")
        var title: String
) : Serializable

@Entity(tableName = "device")
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
        var roomId: String
) : Serializable

class RoomWithDevices {

    @Embedded
    var room: RoomEntity? = null

    @Relation(parentColumn = "id", entityColumn = "room_id", entity = DeviceEntity::class)
    var devices: List<DeviceEntity>? = null
}