package uk.co.seanhodges.incandescent.client.selection

import android.app.Application
import android.os.AsyncTask
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import uk.co.seanhodges.incandescent.client.Inject
import uk.co.seanhodges.incandescent.client.LastValueChangeListener
import uk.co.seanhodges.incandescent.client.storage.AppDatabase
import uk.co.seanhodges.incandescent.client.storage.DeviceDao
import uk.co.seanhodges.incandescent.client.storage.RoomDao
import uk.co.seanhodges.incandescent.client.storage.RoomWithDevices
import uk.co.seanhodges.incandescent.lightwave.event.LWEventPayloadGroup
import uk.co.seanhodges.incandescent.lightwave.server.LightwaveServer


class DeviceSelectViewModel(
        application: Application
) : AndroidViewModel(application) {

    private val roomDao: RoomDao = AppDatabase.getDatabase(application).roomDao()
    private val deviceDao: DeviceDao = AppDatabase.getDatabase(application).deviceDao()
    private val roomsWithDevices: LiveData<List<RoomWithDevices>> = roomDao.loadAllWithDevices()
    private val lastValueChangeListener: LastValueChangeListener = Inject.lastValueChangeListener

    fun listenForValueChanges(owner: LifecycleOwner) {
        lastValueChangeListener.setRepository(owner, deviceDao)
    }

    fun getAllRooms(): LiveData<List<RoomWithDevices>> {
        return roomsWithDevices
    }

    fun initialiseRooms(server: LightwaveServer) {
        InitialiseRoomsTask(roomDao, server).execute()
    }

    private class InitialiseRoomsTask(
            private val roomDao: RoomDao,
            private val server: LightwaveServer
    ) : AsyncTask<Void, Void, Void>() {

        override fun doInBackground(vararg params: Void): Void? {
            if (roomDao.count() < 1) {
                LightwaveConfigLoader(server).load { hierarchy: String, info: LWEventPayloadGroup ->
                    LightwaveConfigParser(hierarchy, info).parse { room, devices ->
                        Log.d(javaClass.name, "Adding room ${room.title} with ${devices.size} devices")
                        roomDao.insertRoomAndDevices(room, devices)
                    }
                }
            }
            return null
        }
    }
}