package uk.co.seanhodges.incandescent.client.fragment.applianceList

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import uk.co.seanhodges.incandescent.client.DeviceChangeHandler
import uk.co.seanhodges.incandescent.client.Inject
import uk.co.seanhodges.incandescent.client.storage.*


class ApplianceListViewModel(
        application: Application
) : AndroidViewModel(application) {

    private val sceneDao: SceneDao = AppDatabase.getDatabase(application).sceneDao()
    private val roomDao: RoomDao = AppDatabase.getDatabase(application).roomDao()
    private val deviceDao: DeviceDao = AppDatabase.getDatabase(application).deviceDao()
    private val deviceChangeHandler: DeviceChangeHandler = Inject.deviceChangeHandler

    fun listenForValueChanges(owner: LifecycleOwner) {
        deviceChangeHandler.setRepository(owner, deviceDao)
    }

    fun getAllScenes(): LiveData<List<SceneWithActions>> {
        return sceneDao.loadAllWithActions()
    }

    fun getAllRooms(): LiveData<List<RoomWithDevices>> {
        return roomDao.loadAllWithDevices()
    }
}