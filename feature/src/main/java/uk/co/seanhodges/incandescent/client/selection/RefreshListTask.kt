package uk.co.seanhodges.incandescent.client.selection

import android.os.AsyncTask
import android.util.Log
import uk.co.seanhodges.incandescent.client.storage.RoomDao
import uk.co.seanhodges.incandescent.lightwave.event.LWEventPayloadGroup
import uk.co.seanhodges.incandescent.lightwave.server.LightwaveServer

class RefreshListTask(
        private val roomDao: RoomDao,
        private val server: LightwaveServer,
        private val callback: () -> Unit
) : AsyncTask<Boolean, Void, Void>() {

    override fun doInBackground(vararg params: Boolean?): Void? {
        Log.d(javaClass.name, "Loading config")
        LightwaveConfigLoader(server).load(params[0] == true) { hierarchy: String, info: LWEventPayloadGroup ->
            Log.d(javaClass.name, "Parsing config")
            LightwaveConfigParser(hierarchy, info).parse { room, devices ->
                Log.d(javaClass.name, "Upserting room ${room.title} with ${devices.size} devices")
                roomDao.insertRoomAndDevices(room, devices)
            }
            callback()
        }
        return null
    }
}