package uk.co.seanhodges.incandescent.client.selection

import android.os.AsyncTask
import android.util.Log
import uk.co.seanhodges.incandescent.client.storage.DeviceDao
import uk.co.seanhodges.incandescent.client.storage.DeviceEntity
import uk.co.seanhodges.incandescent.client.storage.RoomDao
import uk.co.seanhodges.incandescent.client.storage.RoomEntity
import uk.co.seanhodges.incandescent.lightwave.event.LWEventPayloadGroup
import uk.co.seanhodges.incandescent.lightwave.server.LightwaveServer

class RefreshListTask(
        private val roomDao: RoomDao,
        private val deviceDao: DeviceDao,
        private val server: LightwaveServer,
        private val callback: () -> Unit
) {

    fun execute(vararg params: Boolean?): Void? {
        Log.d(javaClass.name, "Loading config")
        LightwaveConfigLoader(server).load(params[0] == true) { hierarchy: String, info: LWEventPayloadGroup ->
            val existingRoomRefs = mutableListOf<String>()
            val existingDeviceRefs = mutableListOf<String>()

            Log.d(javaClass.name, "Parsing config")
            LightwaveConfigParser(hierarchy, info).parse { room, devices ->
                updateExistingRoom(room)

                val existingDevicesInRoom = devices.filter { device ->
                    deviceDao.findById(device.id) != null
                }

                Log.d(javaClass.name, "Inserting room ${room.title} with ${devices.size} devices")
                roomDao.insertRoomAndDevices(room, devices)
                updateExistingDevices(existingDevicesInRoom)

                existingRoomRefs.add(room.id)
                existingDeviceRefs.addAll(devices.map { it.id })
            }

            // Delete rooms and devices that no longer exist
            deviceDao.deleteDevicesNotInList(existingDeviceRefs)
            roomDao.deleteRoomsNotInList(existingRoomRefs)

            callback()
        }
        return null
    }

    private fun updateExistingRoom(room: RoomEntity) {
        val dbRoom = roomDao.findById(room.id)
        if (dbRoom != null && room.title != dbRoom.title) {
            roomDao.updateRoom(room.id, room.title)
        }
    }

    private fun updateExistingDevices(existingDevicesInRoom: List<DeviceEntity>) {
        existingDevicesInRoom.map { device ->
            Log.d(javaClass.name, "Updating device ${device.title}")
            deviceDao.updateDevice(device.id, device.title, device.roomId)
        }
    }
}