package uk.co.seanhodges.incandescent.client.storage

import androidx.lifecycle.LiveData
import androidx.room.*
import java.io.Serializable

@Dao
interface RoomDao {

    @Query("SELECT * FROM room WHERE id = :id")
    fun findById(id: String): RoomEntity?

    @Query("SELECT * FROM room ORDER BY chosen_count DESC, id")
    fun loadAll(): LiveData<List<RoomEntity>>

    @Transaction
    @Query("SELECT * FROM room ORDER BY chosen_count DESC, id")
    fun loadAllWithDevices(): LiveData<List<RoomWithDevices>>

    @Query("SELECT count(id) FROM room")
    fun count(): LiveData<Int>

    @Query("UPDATE room SET chosen_count = chosen_count + 1 WHERE id = :id")
    fun incChosenCount(id: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertRoomAndDevices(room: RoomEntity, devices: List<DeviceEntity>)

    @Query("UPDATE room SET title = :title WHERE id = :id")
    fun updateRoom(id: String, title: String)

    @Query("DELETE FROM room WHERE id NOT IN (:ids)")
    fun deleteRoomsNotInList(ids: List<String>)
}

@Dao
interface DeviceDao {

    @Query("SELECT * FROM device WHERE id = :id")
    fun findById(id: String): LiveData<DeviceEntity>

    @Query("SELECT count(id) FROM device WHERE id = :id")
    fun exists(id: String): Int

    @Query("SELECT * FROM device WHERE dim_command = :commandId OR power_command = :commandId")
    fun findByCommandId(commandId: String): DeviceEntity?

    @Query("SELECT * FROM device WHERE device.room_id = :roomId ORDER BY chosen_count DESC, id")
    fun findByRoom(roomId: String): LiveData<List<DeviceEntity>>

    @Query("SELECT * FROM device INNER JOIN room ON room.id = device.room_id WHERE room.title = :roomName AND device.title = :deviceName")
    fun findByRoomAndDeviceName(roomName: String, deviceName: String) : DeviceEntity

    @Query("UPDATE device SET chosen_count = chosen_count + 1 WHERE id = :id")
    fun incChosenCount(id: String)

    @Query("UPDATE device SET last_value_dim = :value WHERE id = :deviceId")
    fun setLastDimValue(deviceId: String, value : Int)

    @Query("UPDATE device SET last_value_power = :value WHERE id = :deviceId")
    fun setLastPowerValue(deviceId: String, value : Int)

    @Query("UPDATE device SET title = :title, room_id = :roomId WHERE id = :id")
    fun updateDevice(id: String, title: String, roomId: String)

    @Query("DELETE FROM device WHERE id NOT IN (:ids)")
    fun deleteDevicesNotInList(ids: List<String>)
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

) : Serializable {

    @ColumnInfo(name = "last_value_power")
    var lastPowerValue: Int = 0

    @ColumnInfo(name = "last_value_dim")
    var lastDimValue: Int = 0

}

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