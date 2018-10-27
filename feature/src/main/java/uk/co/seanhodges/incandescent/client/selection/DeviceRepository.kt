package uk.co.seanhodges.incandescent.client.selection

import android.content.Context
import android.util.Log
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
        db = Room.databaseBuilder(ctxRef.get()!!,
                AppDatabase::class.java, DATABASE_NAME).build()
//        db = Room.inMemoryDatabaseBuilder(ctxRef.get()!!, AppDatabase::class.java).build()
    }

    fun isNewDB() : Boolean {
        return db.roomDao().count() < 1
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
        val rooms = db.roomDao().loadAllWithDevices()
        val result = rooms.toMutableList()
        var devices = result[0].devices!!.toMutableList()
        devices[0].title = "Main light"
        devices.add(DeviceEntity("test1", "TV", "power", null, null, result[0].devices!![0].roomId))
        devices.add(DeviceEntity("test2", "Record Player", "power", null, null, result[0].devices!![0].roomId))
        devices.add(DeviceEntity("test3", "Power socket", "power", null, null, result[0].devices!![0].roomId))
        result[0].devices = devices

        devices = result[1].devices!!.toMutableList()
        devices[0].title = "Main light"
        devices.add(DeviceEntity("test4", "Bedside lamp", "power", null, null, result[1].devices!![0].roomId))
        result[1].devices = devices

        devices = result[2].devices!!.toMutableList()
        devices[0].title = "Main light"
        devices.add(DeviceEntity("test5", "Counter lights", "power", null, null, result[2].devices!![0].roomId))
        devices.add(DeviceEntity("test6", "Kettle", "power", null, null, result[2].devices!![0].roomId))
        result[2].devices = devices

        return result
    }

    fun addRoomAndDevices(entry: RoomWithDevices) {
        if (entry.room == null) return
        Log.d(javaClass.name, "Adding room ${entry.room?.title} with ${entry.devices?.size} devices")
        db.roomDao().insertRoomAndDevices(entry.room!!, entry.devices ?: emptyList())
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

    @Query("SELECT count(id) FROM room")
    fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
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

data class RoomWithDevices(

    @Embedded
    var room: RoomEntity? = null,

    @Relation(parentColumn = "id", entityColumn = "room_id", entity = DeviceEntity::class)
    var devices: List<DeviceEntity>? = null
)