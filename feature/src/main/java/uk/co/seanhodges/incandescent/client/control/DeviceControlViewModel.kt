package uk.co.seanhodges.incandescent.client.control

import android.app.Application
import android.os.AsyncTask
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import uk.co.seanhodges.incandescent.client.selection.*


class DeviceControlViewModel(
        application: Application
) : AndroidViewModel(application) {

    private val roomDao: RoomDao = AppDatabase.getDatabase(application).roomDao()
    private val deviceDao: DeviceDao = AppDatabase.getDatabase(application).deviceDao()

    fun incChosenCount(roomId: String, deviceId: String) {
        IncChosenCountAsyncTask(roomDao, deviceDao).execute(roomId, deviceId)
    }

    private class IncChosenCountAsyncTask internal constructor(
            private val roomDao: RoomDao,
            private val deviceDao: DeviceDao
    ) : AsyncTask<String, Void, Void>() {

        override fun doInBackground(vararg params: String): Void? {
            Log.d(javaClass.simpleName, "Recording use of ${params[0]} : ${params[1]}")
            roomDao.incChosenCount(params[0])
            deviceDao.incChosenCount(params[1])
            return null
        }

    }
}