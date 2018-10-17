package uk.co.seanhodges.incandescent.client.selection

import android.content.Context
import androidx.room.Database
import androidx.room.*
import java.lang.ref.WeakReference
import androidx.room.Embedded

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
                DeviceEntity("1", "Main light", "light", "1")
        ))
        db.roomDao().insertRoomAndDevices(RoomEntity("2", "Bedroom"), listOf(
                DeviceEntity("2", "Main light", "light", "2")
        ))
    }

    fun getAllRooms(): List<RoomWithDevices> {
        return db.roomDao().loadAllWithDevices()
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
)

@Entity(tableName = "device")
data class DeviceEntity(

        @PrimaryKey
        var id: String,

        @ColumnInfo(name = "title")
        var text: String,

        @ColumnInfo(name = "type")
        var type: String,

        @ForeignKey(entity = RoomEntity::class,
                parentColumns = ["id"],
                childColumns = ["roomId"])
        var roomId: String
)

class RoomWithDevices {

    @Embedded
    var room: RoomEntity? = null

    @Relation(parentColumn = "id", entityColumn = "roomId", entity = DeviceEntity::class)
    var devices: List<DeviceEntity>? = null
}