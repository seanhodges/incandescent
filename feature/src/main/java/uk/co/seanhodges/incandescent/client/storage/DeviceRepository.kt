package uk.co.seanhodges.incandescent.client.storage

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Database
import androidx.room.*
import java.io.Serializable
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import androidx.room.Room

@Dao
interface RoomDao {

    @Transaction
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

    @Query("SELECT * FROM device WHERE dim_command = :commandId OR power_command = :commandId")
    fun findByCommandId(commandId: String): DeviceEntity

    @Query("UPDATE device SET chosen_count = chosen_count + 1 WHERE id = :id")
    fun incChosenCount(id: String)

    @Query("UPDATE device SET last_value_dim = :value WHERE id = :deviceId")
    fun setLastDimValue(deviceId: String, value : Int)

    @Query("UPDATE device SET last_value_power = :value WHERE id = :deviceId")
    fun setLastPowerValue(deviceId: String, value : Int)
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