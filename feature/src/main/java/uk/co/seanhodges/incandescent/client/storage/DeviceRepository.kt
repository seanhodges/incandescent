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

    @Query("SELECT * FROM device WHERE id IN (:ids)")
    fun findByIds(ids: List<String>): List<DeviceEntity>

    @Query("SELECT count(id) FROM device WHERE id = :id")
    fun exists(id: String): Int

    @Query("SELECT * FROM device WHERE dim_command = :commandId OR power_command = :commandId")
    fun findByCommandId(commandId: String): DeviceEntity?

    @Query("SELECT * FROM device WHERE device.room_id = :roomId ORDER BY chosen_count DESC, id")
    fun findByRoom(roomId: String): LiveData<List<DeviceEntity>>

    @Query("SELECT * FROM device INNER JOIN room ON room.id = device.room_id WHERE room.title = :roomName AND device.title = :deviceName")
    fun findByRoomAndDeviceName(roomName: String, deviceName: String) : DeviceEntity

    @Query("SELECT * FROM device WHERE device.type = :type")
    fun findByType(type: String): List<DeviceEntity>

    @Query("UPDATE device SET chosen_count = chosen_count + 1 WHERE id = :id")
    fun incChosenCount(id: String)

    @Query("UPDATE device SET last_value_dim = :value WHERE id = :deviceId")
    fun setLastDimValue(deviceId: String, value : Int)

    @Query("UPDATE device SET last_value_power = :value WHERE id = :deviceId")
    fun setLastPowerValue(deviceId: String, value : Int)

    @Query("UPDATE device SET title = :title, room_id = :roomId, type = :type, hidden = :hidden, power_usage_command = :powerUsageCommand, energy_consumption_command = :energyConsumptionCommand WHERE id = :id")
    fun updateDevice(id: String, title: String, roomId: String, type: String, hidden: Boolean, powerUsageCommand: String?, energyConsumptionCommand: String?)

    @Query("DELETE FROM device WHERE id NOT IN (:ids)")
    fun deleteDevicesNotInList(ids: List<String>)
}

@Dao
interface DeviceFeatureDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertHeatingFeatures(feature: FeaturesetHeatingEntity)

    @Query("SELECT * FROM featureset_heating WHERE device_id = :deviceId")
    fun getHeatingFeatures(deviceId: String): FeaturesetHeatingEntity?

    @Query("DELETE FROM featureset_heating WHERE device_id = :deviceId")
    fun deleteHeatingFeatures(deviceId: String)
}

@Entity(tableName = "room", indices = [Index("chosen_count", name = "idx_room_chosen_count")])
data class RoomEntity(

        @PrimaryKey
        var id: String,

        @ColumnInfo(name = "title")
        var title: String,

        @ColumnInfo(name = "chosen_count")
        var chosenCount: Int = 0
) : Serializable

@Entity(tableName = "device", indices = [Index("chosen_count", name = "idx_device_chosen_count")])
data class DeviceEntity(

        @PrimaryKey
        var id: String,

        @ColumnInfo(name = "title")
        var title: String,

        @ColumnInfo(name = "power_command")
        var powerCommand: String?,

        @ColumnInfo(name = "dim_command")
        var dimCommand: String?,


        @ColumnInfo(name = "power_usage_command")
        var powerUsageCommand: String?,

        @ColumnInfo(name = "energy_consumption_command")
        var energyConsumptionCommand: String?,

        @ForeignKey(entity = RoomEntity::class,
                parentColumns = ["id"],
                childColumns = ["room_id"])
        @ColumnInfo(name = "room_id")
        var roomId: String,

        @ColumnInfo(name = "chosen_count")
        var chosenCount: Int = 0

) : Serializable {

    @ColumnInfo(name = "type")
    var type: String = ""

    @ColumnInfo(name = "last_value_power")
    var lastPowerValue: Int = 0

    @ColumnInfo(name = "last_value_dim")
    var lastDimValue: Int = 0

    @ColumnInfo(name = "hidden")
    var hidden: Boolean = false

    fun inferType() {
        // TODO(sean): add heating support
        type = when {
            !energyConsumptionCommand.isNullOrEmpty() && dimCommand.isNullOrEmpty() && powerCommand.isNullOrEmpty()-> "energy_monitor"
            !dimCommand.isNullOrEmpty() -> "light"
            !powerCommand.isNullOrEmpty() -> "socket"
            else -> "unknown"
        }
    }

}

data class RoomWithDevices(

        @Embedded
        var room: RoomEntity? = null,

        @Relation(parentColumn = "id", entityColumn = "room_id", entity = DeviceEntity::class)
        var devices: List<DeviceEntity>? = null
) {

    fun getVisibleDevices(): List<DeviceEntity> {
        return devices.orEmpty().filter { !it.hidden }.sortedByDescending { it.chosenCount }
    }
}

interface FeaturesetEntity

@Entity(tableName = "featureset_heating")
data class FeaturesetHeatingEntity(

        @PrimaryKey(autoGenerate = true)
        var id: Long = 0,

        @ForeignKey(entity = DeviceEntity::class,
                parentColumns = ["id"],
                childColumns = ["device_id"])
        @ColumnInfo(name = "device_id")
        var deviceId: String,

        @ColumnInfo(name = "current_temp_command")
        var currentTempCmd: String?,

        @ColumnInfo(name = "target_temp_command")
        var targetTempCmd: String?,

        @ColumnInfo(name = "valve_level_command")
        var valveLevelCommand: String?,

        @ColumnInfo(name = "heat_state_command")
        var heatStateCommand: String?,

        @ColumnInfo(name = "battery_level_command")
        var batteryLevelCommand: String?

) : Serializable, FeaturesetEntity {

    @ColumnInfo(name = "current_temp_value")
    var currentTempValue: Int = 0

    @ColumnInfo(name = "target_temp_value")
    var targetTempValue: Int = 0

    @ColumnInfo(name = "heat_state_value")
    var valveLevelValue: Int = 0

    @ColumnInfo(name = "heat_state_value")
    var heatStateValue: Int = 0

    @ColumnInfo(name = "battery_level_value")
    var batteryLevelValue: Int = 0
}