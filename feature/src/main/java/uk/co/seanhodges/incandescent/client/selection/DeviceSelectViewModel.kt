package uk.co.seanhodges.incandescent.client.selection

import android.app.Activity
import android.app.Application
import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import uk.co.seanhodges.incandescent.client.DeviceChangeHandler
import uk.co.seanhodges.incandescent.client.Inject
import uk.co.seanhodges.incandescent.client.storage.AppDatabase
import uk.co.seanhodges.incandescent.client.storage.DeviceDao
import uk.co.seanhodges.incandescent.client.storage.RoomDao
import uk.co.seanhodges.incandescent.client.storage.RoomWithDevices
import uk.co.seanhodges.incandescent.lightwave.server.LightwaveServer
import java.lang.ref.WeakReference


class DeviceSelectViewModel(
        application: Application
) : AndroidViewModel(application) {

    private val roomDao: RoomDao = AppDatabase.getDatabase(application).roomDao()
    private val deviceDao: DeviceDao = AppDatabase.getDatabase(application).deviceDao()
    private val server: LightwaveServer = Inject.server
    private val deviceChangeHandler: DeviceChangeHandler = Inject.deviceChangeHandler

    fun listenForValueChanges(owner: LifecycleOwner) {
        deviceChangeHandler.setRepository(owner, deviceDao)
    }

    fun getAllRooms(): LiveData<List<RoomWithDevices>> {
        return roomDao.loadAllWithDevices()
    }

    fun initialiseList(owner: LifecycleOwner) {
        roomDao.count().observe(owner, Observer<Int>{ count: Int ->
            if (count <= 0) {
                Log.i(javaClass.name, "Initialising list")
                RefreshListTask(roomDao, deviceDao, server) {
                    Log.i(javaClass.name, "Complete")
                }.execute(false)
            }
        })
    }

    fun refreshList(activity: Activity) {
        val activityRef = WeakReference(activity)

        Log.i(javaClass.name, "Refreshing list")
        RefreshListTask(roomDao, deviceDao, server) {
            Log.i(javaClass.name, "Complete")
            activityRef.get()?.let { activity ->
                activity.runOnUiThread {
                    Toast.makeText(activity, "Appliance list updated", Toast.LENGTH_SHORT).show()
                }
            }
        }.execute(true)
    }
}